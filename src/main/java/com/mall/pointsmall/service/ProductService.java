package com.mall.pointsmall.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.entity.ProductCategory;
import com.mall.pointsmall.enums.ProductTransactionType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.ProductCategoryRepository;
import com.mall.pointsmall.repository.ProductRepository;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductTransactionService productTransactionService;
    private final ObjectMapper objectMapper;

    public ProductService(ProductCategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          ProductTransactionService productTransactionService,
                          ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productTransactionService = productTransactionService;
        this.objectMapper = objectMapper;
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
    public Product saveProduct(Product product, SecurityUser actor) {
        normalizeImages(product);
        if (product.getId() != null) {
            Product existing = getProduct(product.getId());
            int before = existing.getStock();
            existing.setCategoryId(product.getCategoryId());
            existing.setName(product.getName());
            existing.setCoverImageUrl(product.getCoverImageUrl());
            existing.setGalleryJson(product.getGalleryJson());
            existing.setPointsCost(product.getPointsCost());
            existing.setStock(product.getStock());
            existing.setEnabled(product.isEnabled());
            existing.setSortOrder(product.getSortOrder());
            existing.setDescription(product.getDescription());
            Product saved = productRepository.save(existing);
            if (before != saved.getStock()) {
                productTransactionService.adminChange(saved, ProductTransactionType.ADMIN_ADJUST,
                        before, saved.getStock(), actor, "员工修改商品库存");
            }
            return saved;
        }
        Product saved = productRepository.save(product);
        productTransactionService.adminChange(saved, ProductTransactionType.INITIAL_STOCK,
                0, saved.getStock(), actor, "新增商品初始化库存");
        return saved;
    }

    private void normalizeImages(Product product) {
        String galleryJson = product.getGalleryJson();
        if (galleryJson == null || galleryJson.isBlank()) {
            product.setGalleryJson("[]");
            product.setCoverImageUrl(product.getCoverImageUrl() == null ? "" : product.getCoverImageUrl());
            return;
        }
        try {
            List<String> images = objectMapper.readValue(galleryJson, new TypeReference<>() {});
            if (images.size() > 6) {
                throw new BusinessException("每个商品最多上传 6 张图片");
            }
            if (images.stream().anyMatch(image -> image == null || image.isBlank())) {
                throw new BusinessException("商品图片地址不正确");
            }
            product.setCoverImageUrl(images.isEmpty()
                    ? (product.getCoverImageUrl() == null ? "" : product.getCoverImageUrl())
                    : images.get(0));
            product.setGalleryJson(objectMapper.writeValueAsString(images));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("商品图片数据格式不正确");
        }
    }

    @Transactional
    public void changeStock(Long id, int delta, SecurityUser actor) {
        Product product = getProduct(id);
        int before = product.getStock();
        int next = product.getStock() + delta;
        if (next < 0) {
            throw new BusinessException("库存不足");
        }
        product.setStock(next);
        productRepository.save(product);
        productTransactionService.adminChange(product, ProductTransactionType.ADMIN_ADJUST,
                before, next, actor, "员工调整商品库存");
    }

    @Transactional
    public Product updateStatus(Long id, boolean enabled) {
        Product product = getProduct(id);
        product.setEnabled(enabled);
        return productRepository.save(product);
    }
}
