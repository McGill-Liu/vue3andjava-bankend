package com.mall.pointsmall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class ProductCategoryDtos {
    @Data
    public static class SaveRequest {
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 100, message = "分类名称不能超过 100 个字符")
        private String name;

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于 0")
        private Integer sortOrder;

        @NotNull(message = "状态不能为空")
        private Boolean enabled;
    }
}
