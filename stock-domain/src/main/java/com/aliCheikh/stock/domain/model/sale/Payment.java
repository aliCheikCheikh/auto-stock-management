package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable payment within a {@link Sale}, including the initial payment or a later repayment.
 * Each entry has its own identity, receiver and timestamp; equal amounts do not imply equal
 * payments.
 */
public final class Payment {

    private final PaymentId paymentId;
    private final Money amount;
    private final UserId receivedBy;
    private final LocalDateTime receivedAt;

    private Payment(PaymentId paymentId, Money amount, UserId receivedBy, LocalDateTime receivedAt) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.receivedBy = Objects.requireNonNull(receivedBy, "receivedBy cannot be null");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt cannot be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A payment amount must be strictly positive");
        }
    }

    /** Creates a payment with a new identity. */
    public static Payment record(Money amount, UserId receivedBy, LocalDateTime receivedAt) {
        return new Payment(PaymentId.generate(), amount, receivedBy, receivedAt);
    }

    /** Reconstitutes a persisted payment with its original identity. */
    public static Payment rehydrate(PaymentId paymentId,
                                    Money amount,
                                    UserId receivedBy,
                                    LocalDateTime receivedAt) {
        return new Payment(paymentId, amount, receivedBy, receivedAt);
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public Money getAmount() {
        return amount;
    }

    public UserId getReceivedBy() {
        return receivedBy;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Payment other = (Payment) o;
        return paymentId.equals(other.paymentId);
    }

    @Override
    public int hashCode() {
        return paymentId.hashCode();
    }

    @Override
    public String toString() {
        return amount + " le " + receivedAt;
    }
}
