package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.stock.LocationId;

import java.util.Objects;

public record TargetLocation(
        LocationId locationId,
        int quantity
) {
    public TargetLocation {
        Objects.requireNonNull(locationId, "locationId cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }
    }
}