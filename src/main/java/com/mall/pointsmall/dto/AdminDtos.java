package com.mall.pointsmall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

public class AdminDtos {
    @Data
    public static class CustomerCreateRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "手机号不能为空")
        private String phone;

        @NotBlank(message = "身份证号不能为空")
        private String idCardNo;

        @NotNull(message = "初始积分不能为空")
        @Min(value = 0, message = "初始积分不能小于 0")
        private Integer initialPoints;
    }

    @Data
    public static class CustomerUpdateRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "手机号不能为空")
        private String phone;

        @NotBlank(message = "身份证号不能为空")
        private String idCardNo;

        @NotBlank(message = "状态不能为空")
        private String status;
    }

    @Data
    public static class CustomerBalanceRequest {
        @NotNull(message = "目标积分不能为空")
        @Min(value = 0, message = "目标积分不能小于 0")
        private Integer targetBalance;

        private String remark;
    }

    @Data
    public static class CustomerResponse {
        private Long id;
        private String name;
        private String phone;
        private String idCardNo;
        private String status;
        private Integer pointsBalance;
    }

    @Data
    public static class ShipOrderRequest {
        @NotBlank(message = "物流公司不能为空")
        private String shippingCompany;

        @NotBlank(message = "物流单号不能为空")
        private String shippingNo;
    }

    @Data
    public static class ProductStockRequest {
        @NotNull(message = "库存变动不能为空")
        private Integer delta;
    }

    @Data
    public static class ProductStatusRequest {
        @NotNull(message = "上架状态不能为空")
        private Boolean enabled;
    }

    @Data
    public static class AdminCreateRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "邮箱不能为空")
        private String email;

        @NotBlank(message = "密码不能为空")
        private String password;

        private Map<String, String> permissions;
    }

    @Data
    public static class AdminUpdateRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "邮箱不能为空")
        private String email;

        private Map<String, String> permissions;
    }

    @Data
    public static class AdminStatusRequest {
        @NotNull(message = "状态不能为空")
        private Boolean enabled;
    }

    @Data
    public static class AdminResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        private String password;
    }

    @Data
    public static class AdminResponse {
        private Long id;
        private String name;
        private String email;
        private boolean enabled;
        private Map<String, String> permissions;
    }
}
