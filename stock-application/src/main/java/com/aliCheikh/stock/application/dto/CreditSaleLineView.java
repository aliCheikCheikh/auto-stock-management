package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.util.UUID;

/**
 * Une ligne de la vente, nommée.
 *
 * <p>Le prix unitaire est celui pratiqué <b>au moment de la vente</b>, tel que la ligne l'a figé :
 * une créance se discute sur ce qui a été facturé, pas sur le tarif du jour.</p>
 */
public record CreditSaleLineView(UUID productId,
                                 String productName,
                                 String productReference,
                                 int quantity,
                                 Money unitPrice,
                                 Money lineTotal) {
}
