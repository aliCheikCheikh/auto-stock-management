package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record StockReceiptImportExecutionCommand(
        UUID importId,
        StockReceiptImportFile file,
        ShopId shopId,
        UserId userId,
        Set<Integer> selectedLineNumbers
) {
    public StockReceiptImportExecutionCommand {
        Objects.requireNonNull(importId, "importId cannot be null");
        Objects.requireNonNull(file, "file cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        selectedLineNumbers = Set.copyOf(Objects.requireNonNull(
                selectedLineNumbers, "selectedLineNumbers cannot be null"));
        if (selectedLineNumbers.isEmpty()) {
            throw new IllegalArgumentException("selectedLineNumbers cannot be empty");
        }
        if (selectedLineNumbers.stream().anyMatch(lineNumber -> lineNumber == null || lineNumber < 2)) {
            throw new IllegalArgumentException("selectedLineNumbers must point after the header");
        }
    }
}
