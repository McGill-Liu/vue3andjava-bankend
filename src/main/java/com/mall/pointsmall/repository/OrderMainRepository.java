package com.mall.pointsmall.repository;

import com.mall.pointsmall.entity.OrderMain;
import com.mall.pointsmall.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderMainRepository extends JpaRepository<OrderMain, Long> {
    List<OrderMain> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<OrderMain> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime time);
    List<OrderMain> findByStatusAndShippedAtBefore(OrderStatus status, LocalDateTime time);
}
