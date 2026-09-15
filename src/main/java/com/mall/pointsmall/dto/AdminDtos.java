package com.mall.pointsmall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class AdminDtos {
    @Data
    public static class CustomerCreateRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 255, message = "客户姓名不能超过 255 个字符")
        private String name;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
        private String phone;

        @NotBlank(message = "身份证号不能为空")
        @Size(max = 64, message = "身份证号不能超过 64 个字符")
        private String idCardNo;

        @NotNull(message = "初始积分不能为空")
        @Min(value = 0, message = "初始积分不能小于 0")
        @Max(value = 1000000000, message = "初始积分过大")
        private Integer initialPoints;
    }

    @Data
    public static class CustomerUpdateRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 255, message = "客户姓名不能超过 255 个字符")
        private String name;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
        private String phone;

        @NotBlank(message = "身份证号不能为空")
        @Size(max = 64, message = "身份证号不能超过 64 个字符")
        private String idCardNo;

        @NotBlank(message = "状态不能为空")
        private String status;
    }

    @Data
    public static class CustomerBalanceRequest {
        @NotNull(message = "目标积分不能为空")
        @Min(value = 0, message = "目标积分不能小于 0")
        @Max(value = 1000000000, message = "目标积分过大")
        private Integer targetBalance;

        @Size(max = 255, message = "积分调整备注不能超过 255 个字符")
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
        private boolean mustChangePassword;
        private String tempPasswordExpiresAt;
        private String loginLockedUntil;
        private boolean wechatBound;
        private List<CustomerAddressResponse> addresses;
    }

    @Data
    public static class CustomerAddressResponse {
        private String recipientName;
        private String recipientPhone;
        private String detailAddress;
        private boolean defaultAddress;
    }

    @Data
    public static class CustomerTemporaryPasswordResponse {
        private Long customerId;
        private String customerName;
        private String temporaryPassword;
        private String expiresAt;
    }

    @Data
    public static class ShipOrderRequest {
        @NotBlank(message = "物流公司不能为空")
        @Size(max = 100, message = "物流公司不能超过 100 个字符")
        private String shippingCompany;

        @NotBlank(message = "物流单号不能为空")
        @Size(max = 100, message = "物流单号不能超过 100 个字符")
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
    public static class ProductSaveRequest {
        private Long id;

        @NotNull(message = "商品分类不能为空")
        private Long categoryId;

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 150, message = "商品名称不能超过 150 个字符")
        private String name;

        @Size(max = 255, message = "商品封面地址不能超过 255 个字符")
        private String coverImageUrl;

        @Size(max = 10000, message = "商品图片数据过长")
        private String galleryJson;

        @NotNull(message = "商品积分价不能为空")
        @Min(value = 0, message = "商品积分价不能小于 0")
        @Max(value = 100000000, message = "商品积分价过大")
        private Integer pointsCost;

        @NotNull(message = "商品库存不能为空")
        @Min(value = 0, message = "商品库存不能小于 0")
        @Max(value = 100000000, message = "商品库存过大")
        private Integer stock;

        @Min(value = 1, message = "每单限购数量至少为 1")
        @Max(value = 999, message = "每单限购数量不能超过 999")
        private Integer perOrderLimit;

        @Min(value = 1, message = "累计限购数量至少为 1")
        @Max(value = 999, message = "累计限购数量不能超过 999")
        private Integer customerTotalLimit;

        @NotNull(message = "商品状态不能为空")
        private Boolean enabled;

        @NotNull(message = "商品排序不能为空")
        @Min(value = 0, message = "商品排序不能小于 0")
        @Max(value = 100000000, message = "商品排序值过大")
        private Integer sortOrder;

        @Size(max = 10000, message = "商品描述不能超过 10000 个字符")
        private String description;
    }

    @Data
    public static class AdminCreateRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "员工姓名不能超过 100 个字符")
        private String name;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 255, message = "邮箱不能超过 255 个字符")
        private String email;

        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^[A-Za-z0-9-]{6,18}$", message = "密码为 6-18 位，且只能包含数字、英文字母和连字符")
        private String password;

        private Map<String, String> permissions;
    }

    @Data
    public static class AdminUpdateRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "员工姓名不能超过 100 个字符")
        private String name;

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 255, message = "邮箱不能超过 255 个字符")
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
        @Pattern(regexp = "^[A-Za-z0-9-]{6,18}$", message = "新密码为 6-18 位，且只能包含数字、英文字母和连字符")
        private String password;
    }

    @Data
    public static class AdminResponse {
        private Long id;
        private String name;
        private String email;
        private String role;
        private boolean enabled;
        private Map<String, String> permissions;
    }
}
