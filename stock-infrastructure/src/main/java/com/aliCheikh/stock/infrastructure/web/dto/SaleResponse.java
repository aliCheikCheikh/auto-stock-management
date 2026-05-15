package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(@NotNull UUID saleId,
                           UUID sellerId,
                           List<SaleLineResponse> lines,
                           MoneyResponse totalAmount,
                           LocalDateTime createdAt) {
}
