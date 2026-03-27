package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.PointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    List<PointsTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
