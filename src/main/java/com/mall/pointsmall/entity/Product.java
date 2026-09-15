package com.mall.pointsmall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product")
public class Product extends BaseEntity {
    @Column(nullable = false)
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String coverImageUrl;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String galleryJson;

    @Column(nullable = false)
    private Integer pointsCost;

    @Column(nullable = false)
    private Integer stock;

    @Column
    private Integer perOrderLimit;

    @Column
    private Integer customerTotalLimit;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;
}
