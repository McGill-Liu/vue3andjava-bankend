package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByEnabledTrueOrderByCreatedAtDesc();
    List<Product> findByCategoryIdAndEnabledTrueOrderByCreatedAtDesc(Long categoryId);
    List<Product> findByNameContainingIgnoreCaseAndEnabledTrueOrderByCreatedAtDesc(String keyword);
    boolean existsByCategoryId(Long categoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id in :ids order by product.id")
    List<Product> findAllByIdForUpdate(List<Long> ids);
}
