package com.mall.pointsmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public class OrderDtos {
    @Data
    public static class CheckoutRequest {
        @NotNull(message = "地址不能为空")
        private Long addressId;

        @NotEmpty(message = "购物车不能为空")
        private List<CheckoutItem> items;
    }

    @Data
    public static class CheckoutItem {
        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }

    @Data
    public static class AddressRequest {
        @NotBlank(message = "姓名不能为空")
        private String recipientName;

        @NotBlank(message = "手机号不能为空")
        private String recipientPhone;

        @NotBlank(message = "详细地址不能为空")
        private String detailAddress;

        private boolean defaultAddress;
    }
}
