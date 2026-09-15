package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AuthDtos;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.security.SecurityUser;
import com.mall.pointsmall.service.AuthService;
import com.mall.pointsmall.service.OperationRecordService;
import com.mall.pointsmall.enums.RoleType;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final OperationRecordService operationRecordService;

    public AuthController(AuthService authService, OperationRecordService operationRecordService) {
        this.authService = authService;
        this.operationRecordService = operationRecordService;
    }

    @PostMapping("/user/login")
    public ApiResponse<AuthDtos.TokenResponse> userLogin(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.loginUser(request));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<AuthDtos.TokenResponse> wechatLogin(@Valid @RequestBody AuthDtos.WechatLoginRequest request) {
        return ApiResponse.ok(authService.loginWechat(request));
    }

    @PostMapping("/admin/login")
    public ApiResponse<AuthDtos.TokenResponse> adminLogin(@Valid @RequestBody AuthDtos.AdminLoginRequest request) {
        return ApiResponse.ok(authService.loginAdmin(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/change-password")
    @Transactional
    public ApiResponse<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        SecurityUser currentUser = SecurityUtils.currentUser();
        authService.changePassword(currentUser, request);
        if (currentUser.getRole() != RoleType.CUSTOMER) {
            operationRecordService.recordSuccess("修改本人密码", "管理员", currentUser.getId(), currentUser.getName(),
                    "本人密码已修改，原登录会话已失效");
        }
        return ApiResponse.ok("修改成功", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(SecurityUtils.currentUser());
        return ApiResponse.ok("已退出登录", null);
    }
}
