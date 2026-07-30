package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Total facturé et cumul encaissé d'une vente, de quoi en dériver le solde. */
public interface SaleSettlementProjection {

    UUID getSaleId();

    BigDecimal getTotalAmount();

    String getCurrency();

    BigDecimal getAmountPaid();
}
