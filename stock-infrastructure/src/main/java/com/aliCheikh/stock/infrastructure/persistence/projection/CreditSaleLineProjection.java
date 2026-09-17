package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Sale line joined to the product catalog for display names. */
public interface CreditSaleLineProjection {

    UUID getProductId();

    String getProductName();

    String getProductReference();

    int getQuantity();

    BigDecimal getUnitPriceAmount();

    BigDecimal getLineTotalAmount();

    String getCurrency();
}
