package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class StockLevelJpaId implements Serializable {

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    protected StockLevelJpaId() {
    }

    private StockLevelJpaId(UUID locationId, UUID productId) {
        this.locationId = Objects.requireNonNull(locationId, "locationId cannot be null");
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
    }

    public static StockLevelJpaId of(UUID locationId, UUID productId) {
        return new StockLevelJpaId(locationId, productId);
    }

    public UUID getLocationId() {
        return locationId;
    }

    public UUID getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockLevelJpaId that)) return false;
        return locationId.equals(that.locationId)
                && productId.equals(that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId, productId);
    }
}