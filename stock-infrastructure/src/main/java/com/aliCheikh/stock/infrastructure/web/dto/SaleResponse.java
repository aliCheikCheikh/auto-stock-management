package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(UUID saleId,
                           UUID sellerId,
                           List<SaleLineResponse> lines,
                           MoneyResponse totalAmount,
                           LocalDateTime createdAt) {
}
