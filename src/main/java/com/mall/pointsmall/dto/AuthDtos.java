package com.mall.pointsmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

public class AuthDtos {
    @Data
    public static class LoginRequest {
        @NotBlank(message = "手机号不能为空")
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
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        @Pattern(regexp = "^[A-Za-z0-9]{6,}$", message = "新密码至少 6 位，且只能包含数字和英文字母")
        private String newPassword;
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
        private String email;
        private String phone;
        private String idCardNo;
        private Integer pointsBalance;
        private String role;
        private Map<String, String> permissions;
        private String accessToken;
        private String refreshToken;
    }
}
