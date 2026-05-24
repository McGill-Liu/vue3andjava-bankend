package com.mall.pointsmall.entity;

import com.mall.pointsmall.enums.PointsActorType;
import com.mall.pointsmall.enums.ProductTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_transaction")
public class ProductTransaction extends BaseEntity {
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ProductTransactionType type;

    @Column(nullable = false)
    private Integer quantityChange;

    @Column(nullable = false)
    private Integer stockBefore;

    @Column(nullable = false)
    private Integer stockAfter;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointsActorType actorType;

    private Long actorId;

    @Column(nullable = false)
    private String actorName;

    private String remark;
}
