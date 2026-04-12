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

    public AuthService(AdminUserRepository adminUserRepository,
                       CustomerUserRepository customerUserRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AdminPermissionService adminPermissionService,
                       PointsAccountRepository pointsAccountRepository) {
        this.adminUserRepository = adminUserRepository;
        this.customerUserRepository = customerUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.adminPermissionService = adminPermissionService;
        this.pointsAccountRepository = pointsAccountRepository;
    }

    @Transactional
    public void register(AuthDtos.RegisterRequest request) {
        if (customerUserRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("手机号已注册");
        }
        if (customerUserRepository.existsByIdCardNo(request.getIdCardNo())) {
            throw new BusinessException("身份证号已注册");
        }

        CustomerUser user = new CustomerUser();
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setIdCardNo(request.getIdCardNo());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customerUserRepository.save(user);
    }

    public AuthDtos.TokenResponse loginUser(AuthDtos.LoginRequest request) {
        CustomerUser user = customerUserRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() == UserStatus.PENDING_APPROVAL) {
            throw new BusinessException("账号待审核");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("账号不可用");
        }
        return buildToken(new SecurityUser(user.getId(), user.getName(), user.getPhone(), RoleType.CUSTOMER, null));
    }

    public AuthDtos.TokenResponse loginAdmin(AuthDtos.AdminLoginRequest request) {
        AdminUser user = adminUserRepository.findByName(request.getName())
                .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        return buildToken(new SecurityUser(
                user.getId(),
                user.getName(),
                null,
                user.getRole(),
                adminPermissionService.resolvedPermissions(user)
        ));
    }

    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        SecurityUser user = tokenProvider.parse(request.getRefreshToken());
        if (!"refresh".equals(tokenProvider.tokenType(request.getRefreshToken()))) {
            throw new BusinessException("refresh token 无效");
        }
        return buildToken(user);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        CustomerUser user = customerUserRepository.findByPhoneAndNameAndIdCardNo(
                        request.getPhone(), request.getName(), request.getIdCardNo())
                .orElseThrow(() -> new BusinessException("信息校验失败，请联系管理员"));
        String suffix = request.getIdCardNo().substring(Math.max(0, request.getIdCardNo().length() - 6));
        user.setPasswordHash(passwordEncoder.encode(suffix));
        customerUserRepository.save(user);
    }

    @Transactional
    public void changePassword(SecurityUser currentUser, AuthDtos.ChangePasswordRequest request) {
        if (currentUser.getRole() == RoleType.CUSTOMER) {
            CustomerUser user = customerUserRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new BusinessException("用户不存在"));

            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                throw new BusinessException("原密码错误");
            }
            if (!user.getPhone().equals(request.getPhone())) {
                throw new BusinessException("手机号校验失败");
            }
            if (!user.getIdCardNo().equals(request.getIdCardNo())) {
                throw new BusinessException("身份证号校验失败");
            }

            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            customerUserRepository.save(user);
            return;
        }

        AdminUser user = adminUserRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException("管理员不存在"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUserRepository.save(user);
    }

    private AuthDtos.TokenResponse buildToken(SecurityUser user) {
        AuthDtos.TokenResponse response = new AuthDtos.TokenResponse();
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole().name());
        response.setPermissions(user.getPermissions());
        response.setAccessToken(tokenProvider.generateAccessToken(user));
        response.setRefreshToken(tokenProvider.generateRefreshToken(user));
        if (user.getRole() == RoleType.CUSTOMER) {
            customerUserRepository.findById(user.getId()).ifPresent(customer -> {
                response.setIdCardNo(customer.getIdCardNo());
                int balance = pointsAccountRepository.findByCustomerId(customer.getId())
                        .map(PointsAccount::getBalance)
                        .orElse(0);
                response.setPointsBalance(balance);
            });
        }
        return response;
    }
}
