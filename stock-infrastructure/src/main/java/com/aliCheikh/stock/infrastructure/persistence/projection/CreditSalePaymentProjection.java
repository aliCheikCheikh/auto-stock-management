package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Un encaissement et l'auteur qui l'a reçu. */
public interface CreditSalePaymentProjection {

    UUID getPaymentId();

    BigDecimal getAmount();

    String getCurrency();

    LocalDateTime getReceivedAt();

    UUID getReceivedById();

    String getReceivedByName();
}
