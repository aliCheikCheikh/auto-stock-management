package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.List;
import java.util.Objects;

public record ReceiveStockCommand(
        String productReference,
        ProductInfo newProductInfo,
        ShopId shopId,
        UserId userId,
        List<TargetLocation> distributions
) {
    public ReceiveStockCommand {
        Objects.requireNonNull(productReference, "productReference cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(distributions, "distributions cannot be null");

        if (productReference.isBlank()) {
            throw new IllegalArgumentException("productReference cannot be blank");
        }

        if (distributions.isEmpty()) {
            throw new IllegalArgumentException("distributions cannot be empty");
        }

        long distinctLocationCount = distributions.stream()
                .map(TargetLocation::locationId)
                .distinct()
                .count();

        if (distinctLocationCount != distributions.size()) {
            throw new IllegalArgumentException("distributions cannot contain duplicate locationId");
        }
    }
}
