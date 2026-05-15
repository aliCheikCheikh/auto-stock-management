package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record SaleLineResponse(UUID productId,
                               int quantity,
                               MoneyResponse unitPrice,
                               MoneyResponse subtotal) {
}
