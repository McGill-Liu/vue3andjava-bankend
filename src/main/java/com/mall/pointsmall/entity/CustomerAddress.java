package com.mall.pointsmall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer_address")
public class CustomerAddress extends BaseEntity {
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String recipientPhone;

    @Column(nullable = false)
    private String detailAddress;

    @Column(nullable = false)
    private boolean defaultAddress;
}
