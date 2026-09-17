package com.aliCheikh.stock.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Debt query criteria. Outstanding debts are ordered oldest first; settled debts by most recent
 * settlement. A null {@code customerId} includes all customers.
 */
public record ListDebtsQuery(int page,
                             int size,
                             DebtStatus status,
                             UUID customerId) {

    /** Upper bound shared with other paginated queries. */
    public static final int MAX_PAGE_SIZE = 200;

    public ListDebtsQuery {
        Objects.requireNonNull(status, "status cannot be null");

        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
