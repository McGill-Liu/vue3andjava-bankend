package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByCustomerId(Long customerId);
}
