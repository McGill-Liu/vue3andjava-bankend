package com.mall.pointsmall.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.entity.ProductCategory;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.ProductCategoryRepository;
import com.mall.pointsmall.repository.ProductRepository;
import com.mall.pointsmall.security.SecurityUser;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {
    @Test
    void productUpdateProducesFieldLevelAuditSummary() {
        ProductCategoryRepository categories = mock(ProductCategoryRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        ProductTransactionService transactions = mock(ProductTransactionService.class);
        ProductCategory category = new ProductCategory();
        category.setId(3L);
        category.setName("礼品");
        when(categories.findById(3L)).thenReturn(Optional.of(category));

        Product existing = product(8L, "旧名称", 100, 9, "旧描述");
        when(products.findByIdForUpdate(8L)).thenReturn(Optional.of(existing));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductService service = new ProductService(categories, products, transactions, new ObjectMapper());
        AdminDtos.ProductSaveRequest request = request(8L, "新名称", 120, 7, "新描述");
        SecurityUser actor = new SecurityUser(2L, "测试员工", null, RoleType.OPERATOR, Map.of(), false);

        String summary = service.saveProduct(request, actor).auditMessage();

        assertTrue(summary.contains("商品名称：“旧名称”→“新名称”"));
        assertTrue(summary.contains("积分价：“100”→“120”"));
        assertTrue(summary.contains("库存：“9”→“7”"));
        assertTrue(summary.contains("描述：“旧描述”→“新描述”"));
    }

    @Test
    void rejectsPerOrderLimitAboveCustomerTotalLimit() {
        ProductService service = new ProductService(mock(ProductCategoryRepository.class), mock(ProductRepository.class),
                mock(ProductTransactionService.class), new ObjectMapper());
        AdminDtos.ProductSaveRequest request = request(null, "限购商品", 10, 10, "");
        request.setPerOrderLimit(6);
        request.setCustomerTotalLimit(5);
        SecurityUser actor = new SecurityUser(2L, "测试员工", null, RoleType.OPERATOR, Map.of(), false);

        assertThrows(BusinessException.class, () -> service.saveProduct(request, actor));
    }

    private Product product(Long id, String name, int pointsCost, int stock, String description) {
        Product product = new Product();
        product.setId(id);
        product.setCategoryId(3L);
        product.setName(name);
        product.setCoverImageUrl("");
        product.setGalleryJson("[]");
        product.setPointsCost(pointsCost);
        product.setStock(stock);
        product.setEnabled(true);
        product.setSortOrder(0);
        product.setDescription(description);
        return product;
    }

    private AdminDtos.ProductSaveRequest request(Long id, String name, int pointsCost, int stock, String description) {
        AdminDtos.ProductSaveRequest request = new AdminDtos.ProductSaveRequest();
        request.setId(id);
        request.setCategoryId(3L);
        request.setName(name);
        request.setCoverImageUrl("");
        request.setGalleryJson("[]");
        request.setPointsCost(pointsCost);
        request.setStock(stock);
        request.setEnabled(true);
        request.setSortOrder(0);
        request.setDescription(description);
        return request;
    }
}
