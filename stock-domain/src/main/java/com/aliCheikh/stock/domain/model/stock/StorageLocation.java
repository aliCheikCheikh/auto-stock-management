package com.aliCheikh.stock.domain.model.stock;

import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.event.ShopFloorLow;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidLowStockIndicatorException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStorageLocationLabelException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;

import java.time.LocalDateTime;
import java.util.*;

public class StorageLocation {

    private final LocationId locationId;
    private final ShopId shopId;
    private final LocationType locationType;
    private final String label;
    private final int lowStockIndicator;
    private final Map<ProductId, StockLevel> stockLevels = new HashMap<>();

    // AJOUT : Mémoire interne des événements générés par l'agrégat
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public StorageLocation(LocationId locationId, ShopId shopId, LocationType locationType, String label, int lowStockIndicator) {
        if (locationId == null) {
            throw new IllegalArgumentException("locationId cannot be null");
        }
        if (shopId == null) {
            throw new IllegalArgumentException("shopId cannot be null");
        }
        if (locationType == null) {
            throw new IllegalArgumentException("locationType cannot be null");
        }
        if (label == null || label.isBlank()) {
            throw new InvalidStorageLocationLabelException(label);
        }
        if (lowStockIndicator < 0) {
            throw new InvalidLowStockIndicatorException(lowStockIndicator);
        }

        this.locationId = locationId;
        this.shopId = shopId;
        this.locationType = locationType;
        this.label = label;
        this.lowStockIndicator = lowStockIndicator;
    }

    public void increaseStock(ProductId productId, int quantityToIncrease) {
        Objects.requireNonNull(productId, "productId cannot be null");
        StorageLocation.requireStrictlyPositiveQuantity(quantityToIncrease);

        StockLevel stockLevel = stockLevels.get(productId);
        StockLevel newStockLevel;
        if (stockLevel == null) {
            newStockLevel = StockLevel.of(productId, quantityToIncrease);
        } else {
            newStockLevel = stockLevel.increase(quantityToIncrease);
        }

        stockLevels.put(productId, newStockLevel);
    }

    public void decreaseStock(ProductId productId, int quantityToDecrease) {
        Objects.requireNonNull(productId, "productId cannot be null");
        StorageLocation.requireStrictlyPositiveQuantity(quantityToDecrease);
        StockLevel stockLevel = stockLevels.get(productId);

        if (stockLevel == null) {
            throw new InsufficientStockException(productId, 0, quantityToDecrease);
        }

        StockLevel newStockLevel = stockLevel.decrease(quantityToDecrease);
        stockLevels.put(productId, newStockLevel);

        // AJOUT : Vérification et émission autonome de l'événement
        if (isShopFloorLow(productId)) {
            this.domainEvents.add(new ShopFloorLow(
                    productId,
                    this.locationId,
                    newStockLevel.getQuantity(),
                    this.lowStockIndicator,
                    LocalDateTime.now()
            ));
        }
    }

    public int getStockLevel(ProductId productId) {
        StockLevel stockLevel = stockLevels.get(productId);
        if (stockLevel == null) {
            return 0;
        }
        return stockLevel.getQuantity();
    }

    private static void requireStrictlyPositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }

    public boolean hasEnoughStock(ProductId productId, int quantity) {
        return this.getStockLevel(productId) >= quantity;
    }

    public boolean isShopFloorLow(ProductId productId) {
        if (this.locationType != LocationType.SHOP_FLOOR) {
            return false;
        }
        return getStockLevel(productId) < this.lowStockIndicator;
    }

    // AJOUT : Récupérer et vider la liste des événements
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public ShopId getShopId() {
        return shopId;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public int getLowStockIndicator() {
        return lowStockIndicator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageLocation that = (StorageLocation) o;
        return locationId.equals(that.locationId) && shopId.equals(that.shopId);
    }

    @Override
    public int hashCode() {
        return locationId.hashCode();
    }
}