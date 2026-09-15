package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from PointsAccount account where account.customerId = :customerId")
    Optional<PointsAccount> findByCustomerIdForUpdate(Long customerId);
}
