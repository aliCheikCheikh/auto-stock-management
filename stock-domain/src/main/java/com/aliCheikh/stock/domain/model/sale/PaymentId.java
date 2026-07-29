package com.aliCheikh.stock.domain.model.sale;

import java.util.Objects;
import java.util.UUID;

public final class PaymentId {

    private final UUID value;

    private PaymentId(UUID value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    public static PaymentId of(UUID value) {
        return new PaymentId(value);
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
        PaymentId that = (PaymentId) o;
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
