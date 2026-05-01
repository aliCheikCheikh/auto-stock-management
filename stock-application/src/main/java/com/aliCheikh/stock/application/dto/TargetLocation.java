package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.stock.LocationId;

public record TargetLocation(
        LocationId locationId,
        int quantity
) {
    public TargetLocation {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0");
        }
    }
}