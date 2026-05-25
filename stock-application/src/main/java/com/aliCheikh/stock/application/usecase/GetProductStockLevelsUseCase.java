package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.dto.ProductStockSummaryView;
import com.aliCheikh.stock.application.port.ProductStockQueryPort;

import java.util.Objects;
import java.util.Optional;

public class GetProductStockLevelsUseCase {
    private final ProductStockQueryPort productStockQueryPort;

    public GetProductStockLevelsUseCase(ProductStockQueryPort productStockQueryPort) {
        this.productStockQueryPort = Objects.requireNonNull(productStockQueryPort, "productStockQueryPort cannot be null");
    }

    public Optional<ProductStockSummaryView> execute(GetProductStockLevelsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");
        return productStockQueryPort.findProductStockSummary(query);
    }
}
