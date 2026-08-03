package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** En-tête d'une vente à crédit : la vente, son vendeur, son client et le cumul encaissé. */
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
