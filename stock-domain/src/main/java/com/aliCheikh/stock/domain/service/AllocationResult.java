package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.model.stock.LocationId;

import java.util.Objects;

public final class AllocationResult {
    private final LocationId locationId;
    private final int quantity;

    private AllocationResult(LocationId locationId, int quantity) {
        this.locationId = Objects.requireNonNull(locationId, "locationId cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        this.quantity = quantity;
    }

    public static AllocationResult of(LocationId locationId, int quantity) {
        return new AllocationResult(locationId, quantity);
    }

    public LocationId getLocationId() {
        return locationId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId, quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AllocationResult that = (AllocationResult) o;
        return locationId.equals(that.locationId) && quantity == that.quantity;
    }


}
