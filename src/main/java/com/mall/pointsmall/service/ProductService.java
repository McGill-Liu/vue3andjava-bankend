package com.mall.pointsmall.service;

import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.entity.ProductCategory;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.ProductCategoryRepository;
import com.mall.pointsmall.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductService(ProductCategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<ProductCategory> categories() {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    public List<Product> publicProducts(Long categoryId, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndEnabledTrueOrderByCreatedAtDesc(keyword);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndEnabledTrueOrderByCreatedAtDesc(categoryId);
        }
        return productRepository.findByEnabledTrueOrderByCreatedAtDesc();
    }

    public List<Product> adminProducts() {
        return productRepository.findAll();
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new BusinessException("商品不存在"));
    }

    @Transactional
    public ProductCategory saveCategory(ProductCategory category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void changeStock(Long id, int delta) {
        Product product = getProduct(id);
        int next = product.getStock() + delta;
        if (next < 0) {
            throw new BusinessException("库存不足");
        }
        product.setStock(next);
        productRepository.save(product);
    }

    @Transactional
    public Product updateStatus(Long id, boolean enabled) {
        Product product = getProduct(id);
        product.setEnabled(enabled);
        return productRepository.save(product);
    }
}
