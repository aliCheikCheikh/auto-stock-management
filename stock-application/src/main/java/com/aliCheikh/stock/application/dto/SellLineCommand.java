package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;

import java.util.Objects;

public record SellLineCommand(ProductId productId, int quantity) {
    public SellLineCommand(ProductId productId, int quantity) {
        Objects.requireNonNull(productId, "productId cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }
        this.productId = productId;
        this.quantity = quantity;
    }
}
