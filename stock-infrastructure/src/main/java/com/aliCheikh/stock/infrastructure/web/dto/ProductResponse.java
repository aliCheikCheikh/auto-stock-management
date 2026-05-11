package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record ProductResponse(UUID productId, String name, String reference, UUID categoryId, MoneyResponse unitPrice,
                              int minimumGlobalThreshold) {
}
