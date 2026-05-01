package com.aliCheikh.stock.domain.model.movement;

import java.util.Objects;
import java.util.UUID;

public final class MovementId {
    private final UUID value;

    private MovementId(UUID value) {
        Objects.requireNonNull(value, "value cannot be null");
        this.value = value;
    }

    public static MovementId of(UUID value) {
        return new MovementId(value);
    }

    public static MovementId generate() {
        return new MovementId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovementId that = (MovementId) o;
        return value.equals(that.value);
    }


    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
