package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;

import java.util.List;
import java.util.Objects;

public record ProductStockSummaryView(ProductId productId,
                                      String productName,
                                      int globalQuantity,
                                      int minimumGlobalThreshold,
                                      boolean belowGlobalThreshold,
                                      List<StockLevelView> byLocation) {
    public ProductStockSummaryView {
        byLocation = List.copyOf(Objects.requireNonNull(byLocation, "byLocation cannot be null"));
    }
}
