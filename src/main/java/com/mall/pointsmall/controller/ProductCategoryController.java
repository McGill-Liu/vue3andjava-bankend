package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.ProductCategoryDtos;
import com.mall.pointsmall.entity.ProductCategory;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.OperationRecordService;
import com.mall.pointsmall.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/product-categories")
public class ProductCategoryController {
    private final ProductService productService;
    private final AdminPermissionService adminPermissionService;
    private final OperationRecordService operationRecordService;

    public ProductCategoryController(ProductService productService, AdminPermissionService adminPermissionService,
                                     OperationRecordService operationRecordService) {
        this.productService = productService;
        this.adminPermissionService = adminPermissionService;
        this.operationRecordService = operationRecordService;
    }

    @GetMapping
    public ApiResponse<?> list() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.PRODUCT_CATEGORIES);
        return ApiResponse.ok(productService.adminCategories());
    }

    @PostMapping
    @Transactional
    public ApiResponse<ProductCategory> create(@Valid @RequestBody ProductCategoryDtos.SaveRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCT_CATEGORIES);
        ProductCategory category = productService.createCategory(request);
        operationRecordService.recordSuccess("新增商品分类", "商品分类", category.getId(), category.getName(), "商品分类已创建");
        return ApiResponse.ok(category);
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<ProductCategory> update(@PathVariable Long id, @Valid @RequestBody ProductCategoryDtos.SaveRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCT_CATEGORIES);
        ProductCategory category = productService.updateCategory(id, request);
        operationRecordService.recordSuccess("修改商品分类", "商品分类", category.getId(), category.getName(),
                category.isEnabled() ? "商品分类已启用" : "商品分类已停用");
        return ApiResponse.ok(category);
    }
}
