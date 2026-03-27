package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByEnabledTrueOrderByCreatedAtDesc();
    List<Product> findByCategoryIdAndEnabledTrueOrderByCreatedAtDesc(Long categoryId);
    List<Product> findByNameContainingIgnoreCaseAndEnabledTrueOrderByCreatedAtDesc(String keyword);
}
