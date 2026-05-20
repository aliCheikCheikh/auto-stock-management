package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;

public record ListStockLevelsQuery(int page,
                                   int size,
                                   ProductId productId,
                                   ShopId shopId,
                                   LocationId locationId,
                                   Boolean belowThreshold) {
    public ListStockLevelsQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
