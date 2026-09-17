package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;

import java.util.Objects;

public record ReceivingEntry(ProductId productId, LocationId locationId, int quantity) {

    public ReceivingEntry {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(locationId, "locationId cannot be null");
        if (quantity <= 0) {

            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }

    public static ReceivingEntry of(ProductId productId, LocationId locationId, int quantity) {
        return new ReceivingEntry(productId, locationId, quantity);
    }
}
