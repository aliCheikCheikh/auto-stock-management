package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record ProductSearchResponse(UUID productId,
                                    String name,
                                    String reference,
                                    MoneyResponse unitPrice) {
}
