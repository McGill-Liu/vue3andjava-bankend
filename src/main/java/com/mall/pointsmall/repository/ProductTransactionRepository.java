package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.ProductTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTransactionRepository extends JpaRepository<ProductTransaction, Long> {
    List<ProductTransaction> findAllByOrderByCreatedAtDesc();
}
