package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record ProductInfoRequest(String name,
                                 String reference,
                                 UUID categoryId,
                                 MoneyRequest unitPrice,
                                 int minimumGlobalThreshold) {
}
