package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Sale response including the balance computed by the domain. */
public record SaleResponse(UUID saleId,
                           UUID sellerId,
                           String sellerName,
                           List<SaleLineResponse> lines,
                           MoneyResponse totalAmount,
                           LocalDateTime createdAt,
                           UUID customerId,
                           MoneyResponse amountPaid,
                           MoneyResponse amountDue) {
}
