package com.aliCheikh.stock.domain.model.sale;

import java.util.Objects;
import java.util.UUID;

public final class SaleId {
    private final UUID value;


    private SaleId(UUID value) {
        Objects.requireNonNull(value, "value cannot be null");
        this.value = value;
    }

    public static SaleId generate() {
        return new SaleId(UUID.randomUUID());
    }

    public static SaleId of(UUID value) {
        return new SaleId(value);
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleId id = (SaleId) o;
        return value.equals(id.value);
    }
}
