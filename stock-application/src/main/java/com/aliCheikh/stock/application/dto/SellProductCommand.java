package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.List;
import java.util.Objects;

public record SellProductCommand(UserId sellerId, ShopId shopId, List<SellLineCommand> lines) {
    public SellProductCommand {
        Objects.requireNonNull(sellerId, "sellerId cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        Objects.requireNonNull(lines, "lines cannot be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("at  least one line is required");
        }
    }
}


