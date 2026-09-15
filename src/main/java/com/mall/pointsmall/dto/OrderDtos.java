package com.mall.pointsmall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class OrderDtos {
    @Data
    public static class CheckoutRequest {
        @NotBlank(message = "订单提交标识不能为空")
        @Size(min = 16, max = 64, message = "订单提交标识长度不正确")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "订单提交标识格式不正确")
        private String checkoutToken;

        @NotNull(message = "地址不能为空")
        private Long addressId;

        @NotEmpty(message = "购物车不能为空")
        @Size(max = 50, message = "单次最多兑换 50 种商品")
        @Valid
        private List<CheckoutItem> items;

        @NotNull(message = "确认的订单积分不能为空")
        @Min(value = 0, message = "确认的订单积分不能小于 0")
        private Integer expectedTotalPoints;

        @NotNull(message = "确认的积分余额不能为空")
        @Min(value = 0, message = "确认的积分余额不能小于 0")
        private Integer expectedBalanceBefore;
    }

    @Data
    public static class CheckoutItem {
        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "商品数量必须大于 0")
        @Max(value = 999, message = "单件商品每次最多兑换 999 个")
        private Integer quantity;
    }

    @Data
    public static class PurchaseAvailabilityRequest {
        @NotEmpty(message = "商品不能为空")
        @Size(max = 50, message = "单次最多查询 50 种商品")
        private List<@NotNull(message = "商品不能为空") Long> productIds;
    }

    public record PurchaseAvailability(Long productId,
                                       Integer perOrderLimit,
                                       Integer customerTotalLimit,
                                       long purchasedQuantity,
                                       Long remainingTotal) {
    }

    @Data
    public static class AddressRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 100, message = "收件人姓名不能超过 100 个字符")
        private String recipientName;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
        private String recipientPhone;

        @NotBlank(message = "详细地址不能为空")
        @Size(max = 255, message = "详细地址不能超过 255 个字符")
        private String detailAddress;

        private boolean defaultAddress;
    }
}
