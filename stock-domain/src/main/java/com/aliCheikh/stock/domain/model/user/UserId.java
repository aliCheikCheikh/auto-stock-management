package com.aliCheikh.stock.domain.model.user;

import java.util.Objects;
import java.util.UUID;

public final class UserId {
    private final UUID value;

    private UserId(UUID value) {
        Objects.requireNonNull(value, "value cannot be null");
        this.value = value;
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return value.equals(userId.value);
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
