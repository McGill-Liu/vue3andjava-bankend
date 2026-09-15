package com.mall.pointsmall.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mall.pointsmall.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "order_main", uniqueConstraints = @UniqueConstraint(
        name = "uk_order_customer_checkout_token",
        columnNames = {"customer_id", "checkout_token"}))
public class OrderMain extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String orderNo;

    @Column(nullable = false)
    private Long customerId;

    @JsonIgnore
    @Column(length = 64)
    private String checkoutToken;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    @Column(nullable = false, length = 64)
    private String customerIdCardNo;

    @Column(nullable = false)
    private Integer totalPoints;

    @Column(nullable = false)
    private Integer balanceBefore;

    @Column(nullable = false)
    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING_SHIPMENT;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String recipientAddress;

    private String shippingCompany;

    private String shippingNo;

    private LocalDateTime shippedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;
}
