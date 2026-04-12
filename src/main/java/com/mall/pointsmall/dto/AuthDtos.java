package com.mall.pointsmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

public class AuthDtos {
    @Data
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class AdminLoginRequest {
        @NotBlank(message = "管理员名称不能为空")
        private String name;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;

        @NotBlank(message = "身份证号不能为空")
        private String idCardNo;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "手机号不能为空")
        private String phone;

        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "身份证号不能为空")
        private String idCardNo;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        private String newPassword;

        private String phone;

        private String idCardNo;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;
    }

    @Data
    public static class TokenResponse {
        private Long userId;
        private String name;
        private String phone;
        private String idCardNo;
        private Integer pointsBalance;
        private String role;
        private Map<String, String> permissions;
        private String accessToken;
        private String refreshToken;
    }
}
