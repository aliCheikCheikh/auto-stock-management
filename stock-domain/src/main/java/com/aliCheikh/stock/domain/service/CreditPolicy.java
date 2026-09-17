package com.aliCheikh.stock.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Shared credit policy for outstanding and settled debts. Open debts age until today; settled
 * debts age until their last payment. Both use the same overdue threshold.
 */
public final class CreditPolicy {

    /** Maximum duration before a debt is considered overdue. */
    public static final int OVERDUE_AFTER_DAYS = 30;

    private CreditPolicy() {
    }

    /** Number of days the debt has been outstanding as of today. */
    public static long daysOutstanding(LocalDateTime saleDate, LocalDateTime now) {
        return daysBetween(saleDate, now);
    }

    /** Whether an outstanding debt exceeds the allowed duration. */
    public static boolean isOverdue(LocalDateTime saleDate, LocalDateTime now) {
        return exceedsTolerance(saleDate, now);
    }

    /** Number of days between the sale and settlement. */
    public static long daysToSettle(LocalDateTime saleDate, LocalDateTime settledAt) {
        return daysBetween(saleDate, settledAt);
    }

    /** Whether the debt was settled after the allowed duration. */
    public static boolean wasSettledLate(LocalDateTime saleDate, LocalDateTime settledAt) {
        return exceedsTolerance(saleDate, settledAt);
    }

    /** Clamp negative durations to zero if the reference timestamp precedes the sale. */
    private static long daysBetween(LocalDateTime saleDate, LocalDateTime reference) {
        Objects.requireNonNull(saleDate, "saleDate cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");

        return Math.max(0, Duration.between(saleDate, reference).toDays());
    }

    private static boolean exceedsTolerance(LocalDateTime saleDate, LocalDateTime reference) {
        return daysBetween(saleDate, reference) > OVERDUE_AFTER_DAYS;
    }
}
