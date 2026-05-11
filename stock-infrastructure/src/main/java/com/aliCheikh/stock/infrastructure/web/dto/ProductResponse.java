package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record ProductResponse(UUID productId, String name, String reference, UUID category, MoneyResponse unitPrice,
                              int minimumGlobalThreshold) {
}
