package com.aliCheikh.stock.domain.model.stock;

import java.util.Objects;
import java.util.UUID;

public final class LocationId {
    private final UUID value;

    private LocationId(UUID value) {
        Objects.requireNonNull(value, "value cannot be null");
        this.value = value;
    }

    public static LocationId of(UUID value) {
        return new LocationId(value);
    }


    public static LocationId generate() {
        return new LocationId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationId locationId = (LocationId) o;
        return value.equals(locationId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
