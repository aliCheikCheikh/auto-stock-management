package com.aliCheikh.stock.domain.model.category;

import java.util.Objects;
import java.util.UUID;

public final class CategoryId {
    private final UUID value;

    private CategoryId(UUID value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    public static CategoryId generate() {
        return new CategoryId(UUID.randomUUID());
    }

    public static CategoryId of(UUID value) {
        return new CategoryId(value);
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryId that = (CategoryId) o;
        return value.equals(that.value);
    }


    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
