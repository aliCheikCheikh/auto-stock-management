package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.dto.ProductStockSummaryView;

import java.util.Optional;

public interface ProductStockQueryPort {
    Optional<ProductStockSummaryView> findProductStockSummary(GetProductStockLevelsQuery query);
}
