package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

/** Une ligne du détail d'une créance, au prix pratiqué le jour de la vente. */
public record CreditSaleLineResponse(UUID productId,
                                     String productName,
                                     String productReference,
                                     int quantity,
                                     MoneyResponse unitPrice,
                                     MoneyResponse lineTotal) {
}
