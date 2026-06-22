package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.util.Objects;

public record UpdateProductCommand(ProductId productId,
                                   String name,
                                   Money unitPrice,
                                   int minimumGlobalThreshold) {
    public UpdateProductCommand {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(unitPrice, "unitPrice cannot be null");

    }
}
