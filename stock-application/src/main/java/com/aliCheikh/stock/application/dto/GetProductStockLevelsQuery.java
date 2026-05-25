package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;

import java.util.Objects;

public record GetProductStockLevelsQuery(ProductId productId,
                                         ShopId shopId) {
    public GetProductStockLevelsQuery {
        Objects.requireNonNull(productId, "productId cannot be null");

    }
}
