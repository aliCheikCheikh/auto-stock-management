package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.util.UUID;

/** Sale line with the product name and the unit price recorded at the time of sale. */
public record CreditSaleLineView(UUID productId,
                                 String productName,
                                 String productReference,
                                 int quantity,
                                 Money unitPrice,
                                 Money lineTotal) {
}
