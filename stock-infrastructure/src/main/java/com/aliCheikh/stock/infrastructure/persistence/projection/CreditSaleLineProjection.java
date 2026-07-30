package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Une ligne de vente jointe au catalogue, pour que l'écran nomme le produit. */
public interface CreditSaleLineProjection {

    UUID getProductId();

    String getProductName();

    String getProductReference();

    int getQuantity();

    BigDecimal getUnitPriceAmount();

    BigDecimal getLineTotalAmount();

    String getCurrency();
}
