package com.aliCheikh.stock.domain.model.shop;

import java.util.Objects;
import java.util.UUID;

public final class ShopId {
    private final UUID value;

    private ShopId(UUID value) {
        Objects.requireNonNull(value, "value cannot be null");
        this.value = value;
    }

    public static ShopId generate() {
        return new ShopId(UUID.randomUUID());
    }

    public static ShopId of(UUID value) {
        return new ShopId(value);
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
        ShopId shopId = (ShopId) o;
        return value.equals(shopId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
