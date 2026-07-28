package com.aliCheikh.stock.domain.model.customer;

import java.util.Objects;
import java.util.UUID;

public final class CustomerId {
    private final UUID value;

    private CustomerId(UUID value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerId that = (CustomerId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
