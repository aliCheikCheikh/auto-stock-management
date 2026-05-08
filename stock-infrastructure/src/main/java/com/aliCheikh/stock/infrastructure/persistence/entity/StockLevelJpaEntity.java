package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "stock_level")
public class StockLevelJpaEntity {

    @EmbeddedId
    private StockLevelJpaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false, insertable = false, updatable = false)
    private StorageLocationJpaEntity location;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected StockLevelJpaEntity() {
    }

    private StockLevelJpaEntity(StorageLocationJpaEntity location, UUID productId, int quantity) {
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(productId, "productId cannot be null");

        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }

        this.id = StockLevelJpaId.of(location.getId(), productId);
        this.location = location;
        this.quantity = quantity;
    }

    public static StockLevelJpaEntity of(StorageLocationJpaEntity location, UUID productId, int quantity) {
        return new StockLevelJpaEntity(location, productId, quantity);
    }

    public StockLevelJpaId getId() {
        return id;
    }

    public StorageLocationJpaEntity getLocation() {
        return location;
    }

    public UUID getProductId() {
        return id.getProductId();
    }

    public int getQuantity() {
        return quantity;
    }
}