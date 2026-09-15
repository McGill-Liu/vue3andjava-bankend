package com.mall.pointsmall.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.pointsmall.entity.Product;
import com.mall.pointsmall.entity.ProductCategory;
import com.mall.pointsmall.dto.AdminDtos;
import com.mall.pointsmall.dto.ProductCategoryDtos;
import com.mall.pointsmall.enums.ProductTransactionType;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.repository.ProductCategoryRepository;
import com.mall.pointsmall.repository.ProductRepository;
import com.mall.pointsmall.security.SecurityUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        return categoryRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    public List<ProductCategory> adminCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    public List<Product> publicProducts(Long categoryId, String keyword) {
        Set<Long> enabledCategoryIds = categoryRepository.findByEnabledTrueOrderBySortOrderAscIdAsc().stream()
                .map(ProductCategory::getId)
                .collect(Collectors.toSet());
        if (keyword != null && !keyword.isBlank()) {
            return productRepository.findByNameContainingIgnoreCaseAndEnabledTrueOrderByCreatedAtDesc(keyword).stream()
                    .filter(product -> enabledCategoryIds.contains(product.getCategoryId())).toList();
        }
        if (categoryId != null) {
            if (!enabledCategoryIds.contains(categoryId)) {
                return List.of();
            }
            return productRepository.findByCategoryIdAndEnabledTrueOrderByCreatedAtDesc(categoryId);
        }
        return productRepository.findByEnabledTrueOrderByCreatedAtDesc().stream()
                .filter(product -> enabledCategoryIds.contains(product.getCategoryId())).toList();
    }

    public List<Product> adminProducts() {
        return productRepository.findAll();
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new BusinessException("商品不存在"));
    }

    public Product getPublicProduct(Long id) {
        Product product = getProduct(id);
        ProductCategory category = categoryRepository.findById(product.getCategoryId()).orElse(null);
        if (!product.isEnabled() || category == null || !category.isEnabled()) {
            throw new BusinessException("商品不存在或已下架");
        }
        return product;
    }

    @Transactional
    public ProductCategory createCategory(ProductCategoryDtos.SaveRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("分类名称已存在");
        }
        ProductCategory category = new ProductCategory();
        applyCategory(category, request);
        return categoryRepository.save(category);
    }

    @Transactional
    public ProductCategory updateCategory(Long id, ProductCategoryDtos.SaveRequest request) {
        ProductCategory category = categoryRepository.findById(id).orElseThrow(() -> new BusinessException("商品分类不存在"));
        String name = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("分类名称已存在");
        }
        applyCategory(category, request);
        return categoryRepository.save(category);
    }

    @Transactional
    public ProductSaveResult saveProduct(AdminDtos.ProductSaveRequest request, SecurityUser actor) {
        if (request.getPerOrderLimit() != null && request.getCustomerTotalLimit() != null
                && request.getPerOrderLimit() > request.getCustomerTotalLimit()) {
            throw new BusinessException("每笔订单限购数量不能大于每位客户累计限购数量");
        }
        ProductCategory requestedCategory = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("请选择有效的商品分类"));
        Product product = toProduct(request);
        normalizeImages(product);
        if (product.getId() != null) {
            Product existing = getProductForUpdate(product.getId());
            List<String> changes = describeChanges(existing, product, requestedCategory);
            int before = existing.getStock();
            existing.setCategoryId(product.getCategoryId());
            existing.setName(product.getName());
            existing.setCoverImageUrl(product.getCoverImageUrl());
            existing.setGalleryJson(product.getGalleryJson());
            existing.setPointsCost(product.getPointsCost());
            existing.setStock(product.getStock());
            existing.setPerOrderLimit(product.getPerOrderLimit());
            existing.setCustomerTotalLimit(product.getCustomerTotalLimit());
            existing.setEnabled(product.isEnabled());
            existing.setSortOrder(product.getSortOrder());
            existing.setDescription(product.getDescription());
            Product saved = productRepository.save(existing);
            if (before != saved.getStock()) {
                productTransactionService.adminChange(saved, ProductTransactionType.ADMIN_ADJUST,
                        before, saved.getStock(), actor, "员工修改商品库存");
            }
            return new ProductSaveResult(saved, changes.isEmpty() ? "未检测到商品字段变化" : String.join("；", changes));
        }
        Product saved = productRepository.save(product);
        productTransactionService.adminChange(saved, ProductTransactionType.INITIAL_STOCK,
                0, saved.getStock(), actor, "新增商品初始化库存");
        String message = "名称：“" + shortText(saved.getName()) + "”；分类：“" + shortText(requestedCategory.getName())
                + "”；积分价：" + saved.getPointsCost() + "；库存：" + saved.getStock()
                + "；每单限购：" + limitText(saved.getPerOrderLimit())
                + "；累计限购：" + limitText(saved.getCustomerTotalLimit())
                + "；状态：" + enabledText(saved.isEnabled()) + "；图片：" + imageSummary(saved);
        return new ProductSaveResult(saved, message);
    }

    private Product toProduct(AdminDtos.ProductSaveRequest request) {
        Product product = new Product();
        product.setId(request.getId());
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setCoverImageUrl(request.getCoverImageUrl() == null ? "" : request.getCoverImageUrl().trim());
        product.setGalleryJson(request.getGalleryJson());
        product.setPointsCost(request.getPointsCost());
        product.setStock(request.getStock());
        product.setPerOrderLimit(request.getPerOrderLimit());
        product.setCustomerTotalLimit(request.getCustomerTotalLimit());
        product.setEnabled(request.getEnabled());
        product.setSortOrder(request.getSortOrder());
        product.setDescription(request.getDescription());
        return product;
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

    private void applyCategory(ProductCategory category, ProductCategoryDtos.SaveRequest request) {
        category.setName(request.getName().trim());
        category.setSortOrder(request.getSortOrder());
        category.setEnabled(request.getEnabled());
    }

    @Transactional
    public ProductStockChange changeStock(Long id, int delta, SecurityUser actor) {
        Product product = getProductForUpdate(id);
        int before = product.getStock();
        int next;
        try {
            next = Math.addExact(product.getStock(), delta);
        } catch (ArithmeticException ex) {
            throw new BusinessException("库存变动数值过大");
        }
        if (next < 0) {
            throw new BusinessException("库存不足");
        }
        product.setStock(next);
        productRepository.save(product);
        productTransactionService.adminChange(product, ProductTransactionType.ADMIN_ADJUST,
                before, next, actor, "员工调整商品库存");
        return new ProductStockChange(product, before, next);
    }

    @Transactional
    public ProductStatusChange updateStatus(Long id, boolean enabled) {
        Product product = getProductForUpdate(id);
        boolean before = product.isEnabled();
        product.setEnabled(enabled);
        return new ProductStatusChange(productRepository.save(product), before, enabled);
    }

    private Product getProductForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException("商品不存在"));
    }

    private List<String> describeChanges(Product before, Product after, ProductCategory requestedCategory) {
        List<String> changes = new ArrayList<>();
        addChange(changes, "商品名称", before.getName(), after.getName());
        if (!Objects.equals(before.getCategoryId(), after.getCategoryId())) {
            String oldCategory = categoryRepository.findById(before.getCategoryId()).map(ProductCategory::getName).orElse("未知分类");
            changes.add("分类：“" + shortText(oldCategory) + "”→“" + shortText(requestedCategory.getName()) + "”");
        }
        addChange(changes, "积分价", before.getPointsCost(), after.getPointsCost());
        addChange(changes, "库存", before.getStock(), after.getStock());
        if (!Objects.equals(before.getPerOrderLimit(), after.getPerOrderLimit())) {
            changes.add("每单限购：" + limitText(before.getPerOrderLimit()) + "→" + limitText(after.getPerOrderLimit()));
        }
        if (!Objects.equals(before.getCustomerTotalLimit(), after.getCustomerTotalLimit())) {
            changes.add("每位客户累计限购：" + limitText(before.getCustomerTotalLimit()) + "→" + limitText(after.getCustomerTotalLimit()));
        }
        if (before.isEnabled() != after.isEnabled()) {
            changes.add("状态：" + enabledText(before.isEnabled()) + "→" + enabledText(after.isEnabled()));
        }
        addChange(changes, "排序", before.getSortOrder(), after.getSortOrder());
        if (!Objects.equals(normalized(before.getGalleryJson()), normalized(after.getGalleryJson()))
                || !Objects.equals(normalized(before.getCoverImageUrl()), normalized(after.getCoverImageUrl()))) {
            changes.add("商品图片：" + imageSummary(before) + "→" + imageSummary(after));
        }
        if (!Objects.equals(normalized(before.getDescription()), normalized(after.getDescription()))) {
            changes.add("描述：“" + shortText(before.getDescription()) + "”→“" + shortText(after.getDescription()) + "”");
        }
        return changes;
    }

    private void addChange(List<String> changes, String label, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(label + "：“" + shortText(before) + "”→“" + shortText(after) + "”");
        }
    }

    private String shortText(Object value) {
        String text = normalized(value);
        return text.length() <= 36 ? text : text.substring(0, 36) + "…";
    }

    private String normalized(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String imageSummary(Product product) {
        List<String> images = new ArrayList<>();
        try {
            if (product.getGalleryJson() != null && !product.getGalleryJson().isBlank()) {
                images.addAll(objectMapper.readValue(product.getGalleryJson(), new TypeReference<List<String>>() {}));
            }
        } catch (Exception ignored) {
            // Invalid image JSON is rejected before a product can be saved.
        }
        if (images.isEmpty() && product.getCoverImageUrl() != null && !product.getCoverImageUrl().isBlank()) {
            images.add(product.getCoverImageUrl());
        }
        if (images.isEmpty()) return "0 张";
        String labels = images.stream().limit(3).map(this::imageFileLabel).collect(Collectors.joining("、"));
        return images.size() + " 张（" + labels + (images.size() > 3 ? "等" : "") + "）";
    }

    private String imageFileLabel(String url) {
        String value = normalized(url);
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) value = value.substring(0, queryIndex);
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex >= 0) value = value.substring(slashIndex + 1);
        return shortText(value.isBlank() ? "图片" : value);
    }

    private String enabledText(boolean enabled) {
        return enabled ? "上架" : "下架";
    }

    private String limitText(Integer limit) {
        return limit == null ? "不限购" : "最多 " + limit + " 件";
    }

    public record ProductSaveResult(Product product, String auditMessage) {}
    public record ProductStockChange(Product product, int stockBefore, int stockAfter) {}
    public record ProductStatusChange(Product product, boolean enabledBefore, boolean enabledAfter) {}
}
