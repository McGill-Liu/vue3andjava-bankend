package com.mall.pointsmall.service;

import com.mall.pointsmall.dto.AuthDtos;
import com.mall.pointsmall.entity.AdminUser;
import com.mall.pointsmall.entity.CustomerUser;
import com.mall.pointsmall.entity.PointsAccount;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.enums.UserStatus;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.AdminUserRepository;
import com.mall.pointsmall.repository.CustomerUserRepository;
import com.mall.pointsmall.repository.PointsAccountRepository;
import com.mall.pointsmall.security.JwtTokenProvider;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AdminUserRepository adminUserRepository;
    private final CustomerUserRepository customerUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AdminPermissionService adminPermissionService;
    private final PointsAccountRepository pointsAccountRepository;
    private final CustomerSessionService customerSessionService;

    public AuthService(AdminUserRepository adminUserRepository,
                       CustomerUserRepository customerUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AdminPermissionService adminPermissionService,
                       PointsAccountRepository pointsAccountRepository,
                       CustomerSessionService customerSessionService) {
        this.adminUserRepository = adminUserRepository;
        this.customerUserRepository = customerUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.adminPermissionService = adminPermissionService;
        this.pointsAccountRepository = pointsAccountRepository;
        this.customerSessionService = customerSessionService;
    }

    public AuthDtos.TokenResponse loginUser(AuthDtos.LoginRequest request) {
        CustomerUser user = customerUserRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("账号不可用");
        }
        SecurityUser principal = new SecurityUser(user.getId(), user.getName(), user.getPhone(), RoleType.CUSTOMER, null);
        String sessionId = customerSessionService.create(user.getId());
        AuthDtos.TokenResponse response = baseResponse(principal);
        response.setAccessToken(tokenProvider.generateCustomerAccessToken(principal, sessionId));
        response.setRefreshToken(null);
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
                user.getId(), user.getName(), null, user.getRole(), adminPermissionService.resolvedPermissions(user));
        return adminTokens(principal);
    }

    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        SecurityUser user = tokenProvider.parse(request.getRefreshToken());
        if (!"refresh".equals(tokenProvider.tokenType(request.getRefreshToken())) || user.getRole() == RoleType.CUSTOMER) {
            throw new BusinessException("refresh token 无效");
        }
        return adminTokens(user);
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
            customerUserRepository.save(user);
            return;
        }
        AdminUser user = adminUserRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("员工不存在"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(user);
    }

    public void logout(SecurityUser currentUser) {
        if (currentUser.getRole() == RoleType.CUSTOMER) {
            customerSessionService.logout(currentUser.getId());
        }
    }

    private AuthDtos.TokenResponse adminTokens(SecurityUser user) {
        AuthDtos.TokenResponse response = baseResponse(user);
        response.setAccessToken(tokenProvider.generateAccessToken(user));
        response.setRefreshToken(tokenProvider.generateRefreshToken(user));
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
        return response;
    }
}
