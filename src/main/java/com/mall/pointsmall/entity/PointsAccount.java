package com.mall.pointsmall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "points_account")
public class PointsAccount extends BaseEntity {
    @Column(nullable = false, unique = true)
    private Long customerId;

    @Column(nullable = false)
    private Integer balance = 0;
}
