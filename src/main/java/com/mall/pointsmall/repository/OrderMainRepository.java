package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderMainRepository extends JpaRepository<OrderMain, Long> {
    List<OrderMain> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    Optional<OrderMain> findByCustomerIdAndCheckoutToken(Long customerId, String checkoutToken);
    List<OrderMain> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime time);
    List<OrderMain> findByStatusAndShippedAtBefore(OrderStatus status, LocalDateTime time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orderEntity from OrderMain orderEntity where orderEntity.id = :id")
    Optional<OrderMain> findByIdForUpdate(Long id);
}
