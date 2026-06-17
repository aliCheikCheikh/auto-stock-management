package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "product")
public class ProductJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "reference", nullable = false, unique = true, length = 100)
    private String reference;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "minimum_global_threshold", nullable = false)
    private int minimumGlobalThreshold;

    @Column(name = "unit_price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(name = "unit_price_currency", nullable = false, length = 3)
    private String unitPriceCurrency;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ProductJpaEntity() {
    }

    private ProductJpaEntity(
            UUID id,
            String name,
            String reference,
            UUID categoryId,
            int minimumGlobalThreshold,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            boolean active
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.reference = Objects.requireNonNull(reference, "reference cannot be null");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId cannot be null");
        this.minimumGlobalThreshold = minimumGlobalThreshold;
        this.unitPriceAmount = Objects.requireNonNull(unitPriceAmount, "unitPriceAmount cannot be null");
        this.unitPriceCurrency = Objects.requireNonNull(unitPriceCurrency, "unitPriceCurrency cannot be null");
        this.active = active;
    }

    public static ProductJpaEntity of(
            UUID id,
            String name,
            String reference,
            UUID categoryId,
            int minimumGlobalThreshold,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            boolean active
    ) {
        return new ProductJpaEntity(
                id,
                name,
                reference,
                categoryId,
                minimumGlobalThreshold,
                unitPriceAmount,
                unitPriceCurrency,
                active
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public int getMinimumGlobalThreshold() {
        return minimumGlobalThreshold;
    }

    public BigDecimal getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public String getUnitPriceCurrency() {
        return unitPriceCurrency;
    }

    public boolean isActive() {
        return active;
    }
}
