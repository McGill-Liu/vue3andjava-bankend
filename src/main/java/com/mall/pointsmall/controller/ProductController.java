package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.entity.ProductCategory;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;
    private final AdminPermissionService adminPermissionService;

    public ProductController(ProductService productService, AdminPermissionService adminPermissionService) {
        this.productService = productService;
        this.adminPermissionService = adminPermissionService;
    }

    @GetMapping("/categories")
    public ApiResponse<?> categories() {
        return ApiResponse.ok(productService.categories());
    }

    @GetMapping("/products")
    public ApiResponse<?> products(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(productService.publicProducts(categoryId, keyword));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<Product> product(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProduct(id));
    }

    @PostMapping("/admin/categories")
    public ApiResponse<ProductCategory> saveCategory(@RequestBody ProductCategory category) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        return ApiResponse.ok(productService.saveCategory(category));
    }

    @GetMapping("/admin/products")
    public ApiResponse<?> adminProducts() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        return ApiResponse.ok(productService.adminProducts());
    }

    @PostMapping("/admin/products")
    public ApiResponse<Product> saveProduct(@RequestBody Product product) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        return ApiResponse.ok(productService.saveProduct(product));
    }

    @PostMapping("/admin/products/{id}/stock")
    public ApiResponse<Void> updateStock(@PathVariable Long id, @Valid @RequestBody AdminDtos.ProductStockRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        productService.changeStock(id, request.getDelta());
        return ApiResponse.ok("库存已调整", null);
    }

    @PostMapping("/admin/products/{id}/status")
    public ApiResponse<Product> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminDtos.ProductStatusRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        return ApiResponse.ok(productService.updateStatus(id, request.getEnabled()));
    }
}
