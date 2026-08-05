package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.Objects;
import java.util.UUID;

public record StockReceiptImportExecutionDescriptor(
        UUID importId,
        String fingerprint,
        ShopId shopId,
        UserId userId
) {
    public StockReceiptImportExecutionDescriptor {
        Objects.requireNonNull(importId, "importId cannot be null");
        Objects.requireNonNull(fingerprint, "fingerprint cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        if (!fingerprint.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("fingerprint must be a lowercase SHA-256 value");
        }
    }
}
