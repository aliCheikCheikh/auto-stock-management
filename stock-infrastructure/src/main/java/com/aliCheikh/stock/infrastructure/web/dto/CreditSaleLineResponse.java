package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

/** Credit sale line using the price recorded at the time of sale. */
public record CreditSaleLineResponse(UUID productId,
                                     String productName,
                                     String productReference,
                                     int quantity,
                                     MoneyResponse unitPrice,
                                     MoneyResponse lineTotal) {
}
