package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Credit sale header with seller, customer and total payments. */
public interface CreditSaleHeaderProjection {

    UUID getSaleId();

    LocalDateTime getOccurredAt();

    UUID getSellerId();

    String getSellerName();

    UUID getCustomerId();

    String getCustomerGivenName();

    String getCustomerFatherName();

    String getCustomerPhoneNumber();

    BigDecimal getTotalAmount();

    String getCurrency();

    BigDecimal getAmountPaid();
}
