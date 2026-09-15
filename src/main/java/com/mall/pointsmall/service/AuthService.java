package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AuthDtos;
import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.entity.PointsAccount;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.enums.UserStatus;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.exception.LoginFailureException;
import com.mall.pointsmall.repository.AdminUserRepository;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.PointsAccountRepository;
import com.mall.pointsmall.security.JwtTokenProvider;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final AdminUserRepository adminUserRepository;
    private final CustomerUserRepository customerUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AdminPermissionService adminPermissionService;
    private final PointsAccountRepository pointsAccountRepository;
    private final CustomerSessionService customerSessionService;
    private final AdminSessionService adminSessionService;
    private final WeChatMiniProgramService weChatMiniProgramService;

    public AuthService(AdminUserRepository adminUserRepository,
                       CustomerUserRepository customerUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AdminPermissionService adminPermissionService,
                       PointsAccountRepository pointsAccountRepository,
                       CustomerSessionService customerSessionService,
                       AdminSessionService adminSessionService,
                       WeChatMiniProgramService weChatMiniProgramService) {
        this.adminUserRepository = adminUserRepository;
        this.customerUserRepository = customerUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.adminPermissionService = adminPermissionService;
        this.pointsAccountRepository = pointsAccountRepository;
        this.customerSessionService = customerSessionService;
        this.adminSessionService = adminSessionService;
        this.weChatMiniProgramService = weChatMiniProgramService;
    }

    @Transactional(dontRollbackOn = LoginFailureException.class)
    public AuthDtos.TokenResponse loginUser(AuthDtos.LoginRequest request) {
        CustomerUser user = customerUserRepository.findByPhoneForUpdate(request.getPhone())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        LocalDateTime now = LocalDateTime.now();
        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(now)) {
            throw new LoginFailureException("密码连续错误 5 次，账号已锁定至 " + user.getLoginLockedUntil());
        }
        if (user.getLoginLockedUntil() != null) {
            user.setLoginLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLoginLockedUntil(now.plusMinutes(15));
                customerUserRepository.save(user);
                throw new LoginFailureException("密码连续错误 5 次，账号已锁定 15 分钟");
            }
            customerUserRepository.save(user);
            throw new LoginFailureException("密码错误，还可尝试 " + (5 - attempts) + " 次");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("账号不可用");
        }
        if (user.isMustChangePassword() && user.getTempPasswordExpiresAt() != null
                && user.getTempPasswordExpiresAt().isBefore(now)) {
            throw new BusinessException("临时密码已过期，请联系管理员重置");
        }
        user.setFailedLoginAttempts(0);
        user.setLoginLockedUntil(null);
        customerUserRepository.save(user);
        if (!user.isMustChangePassword()) {
            weChatMiniProgramService.tryExchangeCode(request.getWechatCode()).ifPresent(openId -> bindWechatOpenId(user, openId));
        }
        SecurityUser principal = new SecurityUser(user.getId(), user.getName(), user.getPhone(), RoleType.CUSTOMER, null,
                user.isMustChangePassword());
        String sessionId = customerSessionService.create(user.getId());
        AuthDtos.TokenResponse response = baseResponse(principal);
        response.setAccessToken(tokenProvider.generateCustomerAccessToken(principal, sessionId));
        response.setRefreshToken(null);
        response.setIdCardNo(user.getIdCardNo());
        response.setPointsBalance(pointsAccountRepository.findByCustomerId(user.getId()).map(PointsAccount::getBalance).orElse(0));
        return response;
    }

    @Transactional
    public AuthDtos.TokenResponse loginWechat(AuthDtos.WechatLoginRequest request) {
        String openId = weChatMiniProgramService.exchangeCode(request.getCode());
        CustomerUser user = customerUserRepository.findByWechatOpenId(openId)
                .orElseThrow(() -> new BusinessException("此微信尚未绑定客户账号，请先使用手机号和密码登录"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("账号不可用");
        }
        if (user.isMustChangePassword()) {
            throw new BusinessException("请先使用临时密码登录并修改密码");
        }
        SecurityUser principal = new SecurityUser(user.getId(), user.getName(), user.getPhone(), RoleType.CUSTOMER, null, false);
        String sessionId = customerSessionService.create(user.getId());
        AuthDtos.TokenResponse response = baseResponse(principal);
        response.setAccessToken(tokenProvider.generateCustomerAccessToken(principal, sessionId));
        response.setIdCardNo(user.getIdCardNo());
        response.setPointsBalance(pointsAccountRepository.findByCustomerId(user.getId()).map(PointsAccount::getBalance).orElse(0));
        return response;
    }

    public AuthDtos.TokenResponse loginAdmin(AuthDtos.AdminLoginRequest request) {
        AdminUser user = adminUserRepository.findByName(request.getName())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        SecurityUser principal = new SecurityUser(
                user.getId(), user.getName(), null, user.getRole(), adminPermissionService.resolvedPermissions(user), false);
        return adminTokens(principal);
    }

    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        SecurityUser user = tokenProvider.parse(request.getRefreshToken());
        if (!"refresh".equals(tokenProvider.tokenType(request.getRefreshToken())) || user.getRole() == RoleType.CUSTOMER) {
            throw new BusinessException("refresh token 无效");
        }
        if (!adminSessionService.isActive(user.getId(), tokenProvider.sessionId(request.getRefreshToken()))) {
            throw new BusinessException("refresh token 已失效");
        }
        AdminUser admin = adminUserRepository.findById(user.getId())
                .filter(AdminUser::isEnabled)
                .orElseThrow(() -> new BusinessException("账号不可用"));
        SecurityUser principal = new SecurityUser(
                admin.getId(), admin.getName(), null, admin.getRole(), adminPermissionService.resolvedPermissions(admin), false);
        return adminTokens(principal);
    }

    @Transactional
    public void changePassword(SecurityUser currentUser, AuthDtos.ChangePasswordRequest request) {
        if (currentUser.getRole() == RoleType.CUSTOMER) {
            CustomerUser user = customerUserRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new BusinessException("客户不存在"));
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                throw new BusinessException("原密码错误");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setMustChangePassword(false);
            user.setTempPasswordExpiresAt(null);
            user.setFailedLoginAttempts(0);
            user.setLoginLockedUntil(null);
            customerUserRepository.save(user);
            customerSessionService.logout(user.getId());
            return;
        }
        AdminUser user = adminUserRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("员工不存在"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(user);
        adminSessionService.revoke(user.getId());
    }

    public void logout(SecurityUser currentUser) {
        if (currentUser.getRole() == RoleType.CUSTOMER) {
            customerSessionService.logout(currentUser.getId());
            return;
        }
        adminSessionService.revoke(currentUser.getId());
    }

    private AuthDtos.TokenResponse adminTokens(SecurityUser user) {
        String sessionId = adminSessionService.create(user.getId());
        AuthDtos.TokenResponse response = baseResponse(user);
        response.setAccessToken(tokenProvider.generateAccessToken(user, sessionId));
        response.setRefreshToken(tokenProvider.generateRefreshToken(user, sessionId));
        adminUserRepository.findById(user.getId()).ifPresent(admin -> response.setEmail(admin.getEmail()));
        return response;
    }

    private AuthDtos.TokenResponse baseResponse(SecurityUser user) {
        AuthDtos.TokenResponse response = new AuthDtos.TokenResponse();
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole().name());
        response.setPermissions(user.getPermissions());
        response.setMustChangePassword(user.isPasswordChangeRequired());
        return response;
    }

    private void bindWechatOpenId(CustomerUser user, String openId) {
        customerUserRepository.findByWechatOpenId(openId)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("此微信已绑定其他客户账号，请联系管理员处理");
                });
        if (!openId.equals(user.getWechatOpenId())) {
            user.setWechatOpenId(openId);
            user.setWechatBoundAt(LocalDateTime.now());
            customerUserRepository.save(user);
        }
    }
}
