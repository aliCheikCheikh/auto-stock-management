package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Typed Spring Data projection using column aliases rather than positional Object arrays. */
public interface DebtProjection {

    UUID getSaleId();

    LocalDateTime getOccurredAt();

    UUID getCustomerId();

    String getCustomerGivenName();

    String getCustomerFatherName();

    String getCustomerPhoneNumber();

    BigDecimal getTotalAmount();

    String getCurrency();

    BigDecimal getAmountPaid();

    /** Latest payment timestamp, or null if no payment exists. */
    LocalDateTime getLastPaymentAt();
}
