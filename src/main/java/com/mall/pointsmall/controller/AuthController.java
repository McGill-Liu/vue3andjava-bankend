package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AuthDtos;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user/register")
    public ApiResponse<Void> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok("注册成功，等待审核", null);
    }

    @PostMapping("/user/login")
    public ApiResponse<AuthDtos.TokenResponse> userLogin(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.loginUser(request));
    }

    @PostMapping("/admin/login")
    public ApiResponse<AuthDtos.TokenResponse> adminLogin(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok(authService.loginAdmin(request));
    }

    @PostMapping("/user/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("密码已重置", null);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.TokenResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentUser(), request);
        return ApiResponse.ok("修改成功", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok("已退出登录", null);
    }
}
