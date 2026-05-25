package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record ProductStockSummaryResponse(UUID productId,
                                          String productName,
                                          int globalQuantity,
                                          int minimumGlobalThreshold,
                                          boolean belowGlobalThreshold,
                                          List<StockLevelResponse> byLocation) {
}
