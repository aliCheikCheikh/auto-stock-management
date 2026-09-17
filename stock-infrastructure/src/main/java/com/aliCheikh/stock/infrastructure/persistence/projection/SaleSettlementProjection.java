package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Sale total and payments used to derive its balance. */
public interface SaleSettlementProjection {

    UUID getSaleId();

    BigDecimal getTotalAmount();

    String getCurrency();

    BigDecimal getAmountPaid();
}
