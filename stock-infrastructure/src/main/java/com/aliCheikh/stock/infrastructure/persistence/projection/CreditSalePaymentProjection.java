package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Payment and the user who received it. */
public interface CreditSalePaymentProjection {

    UUID getPaymentId();

    BigDecimal getAmount();

    String getCurrency();

    LocalDateTime getReceivedAt();

    UUID getReceivedById();

    String getReceivedByName();
}
