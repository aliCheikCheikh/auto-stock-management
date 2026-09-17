package com.aliCheikh.stock.domain.model.movement;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity shared by stock movements from the same receipt, transfer or sale. Explicit grouping
 * avoids merging separate operations performed close together by the same user.
 */
public final class OperationId {

    private final UUID value;

    private OperationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    public static OperationId generate() {
        return new OperationId(UUID.randomUUID());
    }

    public static OperationId of(UUID value) {
        return new OperationId(value);
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
        OperationId that = (OperationId) o;
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
