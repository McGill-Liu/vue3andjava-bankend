package com.mall.pointsmall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "operation_record")
public class OperationRecord extends BaseEntity {
    private Long operatorId;

    @Column(length = 100)
    private String operatorName;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 64)
    private String targetType;

    private Long targetId;

    @Column(length = 255)
    private String targetName;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 500)
    private String message;

    @Column(length = 64)
    private String requestIp;
}
