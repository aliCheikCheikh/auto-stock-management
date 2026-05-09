package com.aliCheikh.stock.infrastructure.persistence.entity;

import com.aliCheikh.stock.domain.model.stock.LocationType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;


import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "storage_location")
public class StorageLocationJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 50)
    private LocationType locationType;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "low_stock_indicator", nullable = false)
    private int lowStockIndicator;

    @OneToMany(
            mappedBy = "location",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<StockLevelJpaEntity> stockLevels = new HashSet<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected StorageLocationJpaEntity() {
    }

    private StorageLocationJpaEntity(
            UUID id,
            UUID shopId,
            LocationType locationType,
            String label,
            int lowStockIndicator
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.shopId = Objects.requireNonNull(shopId, "shopId cannot be null");
        this.locationType = Objects.requireNonNull(locationType, "locationType cannot be null");
        this.label = Objects.requireNonNull(label, "label cannot be null");
        this.lowStockIndicator = lowStockIndicator;
    }

    public static StorageLocationJpaEntity of(
            UUID id,
            UUID shopId,
            LocationType locationType,
            String label,
            int lowStockIndicator
    ) {
        return new StorageLocationJpaEntity(id, shopId, locationType, label, lowStockIndicator);
    }

    public void replaceStockLevels(Set<StockLevelJpaEntity> stockLevels) {
        this.stockLevels.clear();
        this.stockLevels.addAll(Objects.requireNonNull(stockLevels, "stockLevels cannot be null"));
    }

    public UUID getId() {
        return id;
    }

    public UUID getShopId() {
        return shopId;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public String getLabel() {
        return label;
    }

    public int getLowStockIndicator() {
        return lowStockIndicator;
    }

    public Set<StockLevelJpaEntity> getStockLevels() {
        return stockLevels;
    }

    public Long getVersion() {
        return version;
    }

}