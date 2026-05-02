package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.Objects;

public record TransferStockCommand(
        ProductId productId,
        LocationId sourceLocationId,
        LocationId destinationLocationId,
        int quantity,
        UserId userId
) {
    public TransferStockCommand {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(sourceLocationId, "sourceLocationId cannot be null");
        Objects.requireNonNull(destinationLocationId, "destinationLocationId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }
    }
}