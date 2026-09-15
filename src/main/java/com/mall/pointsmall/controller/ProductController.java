package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import com.mall.pointsmall.service.OperationRecordService;
import com.mall.pointsmall.service.ProductService;
import com.mall.pointsmall.service.ProductTransactionService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;
    private final AdminPermissionService adminPermissionService;
    private final ProductTransactionService productTransactionService;
    private final OperationRecordService operationRecordService;

    public ProductController(ProductService productService,
                             AdminPermissionService adminPermissionService,
                             ProductTransactionService productTransactionService,
                             OperationRecordService operationRecordService) {
        this.productService = productService;
        this.adminPermissionService = adminPermissionService;
        this.productTransactionService = productTransactionService;
        this.operationRecordService = operationRecordService;
    }

    @GetMapping("/categories")
    public ApiResponse<?> categories() {
        return ApiResponse.ok(productService.categories());
    }

    @GetMapping("/products")
    public ApiResponse<?> products(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                   @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.ok(productService.publicProducts(categoryId, keyword));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<Product> product(@PathVariable("id") Long id) {
        return ApiResponse.ok(productService.getPublicProduct(id));
    }

    @GetMapping("/admin/products")
    public ApiResponse<?> adminProducts() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        return ApiResponse.ok(productService.adminProducts());
    }

    @PostMapping("/admin/products")
    @Transactional
    public ApiResponse<Product> saveProduct(@Valid @RequestBody AdminDtos.ProductSaveRequest product) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        boolean creating = product.getId() == null;
        ProductService.ProductSaveResult result = productService.saveProduct(product, SecurityUtils.currentUser());
        Product saved = result.product();
        operationRecordService.recordSuccess(creating ? "新增商品" : "修改商品", "商品", saved.getId(), saved.getName(),
                result.auditMessage());
        return ApiResponse.ok(saved);
    }

    @PostMapping("/admin/products/{id}/stock")
    @Transactional
    public ApiResponse<Void> updateStock(@PathVariable("id") Long id, @Valid @RequestBody AdminDtos.ProductStockRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        ProductService.ProductStockChange change = productService.changeStock(id, request.getDelta(), SecurityUtils.currentUser());
        Product product = change.product();
        operationRecordService.recordSuccess("调整商品库存", "商品", product.getId(), product.getName(),
                "库存：" + change.stockBefore() + "→" + change.stockAfter()
                        + "（变动 " + (request.getDelta() >= 0 ? "+" : "") + request.getDelta() + "）");
        return ApiResponse.ok("库存已调整", null);
    }

    @PostMapping("/admin/products/{id}/status")
    @Transactional
    public ApiResponse<Product> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody AdminDtos.ProductStatusRequest request) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        ProductService.ProductStatusChange change = productService.updateStatus(id, request.getEnabled());
        Product product = change.product();
        operationRecordService.recordSuccess("修改商品状态", "商品", product.getId(), product.getName(),
                "状态：" + (change.enabledBefore() ? "上架" : "下架") + "→" + (change.enabledAfter() ? "上架" : "下架"));
        return ApiResponse.ok(product);
    }

    @GetMapping("/admin/product-transactions")
    public ApiResponse<?> productTransactions() {
        adminPermissionService.assertView(SecurityUtils.currentUser(), AdminMenuKey.PRODUCT_TRANSACTIONS);
        return ApiResponse.ok(productTransactionService.list());
    }
}
