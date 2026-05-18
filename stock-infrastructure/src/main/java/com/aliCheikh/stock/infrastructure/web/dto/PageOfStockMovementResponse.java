package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;

public record PageOfStockMovementResponse(
        List<StockMovementResponse> content,
        PageMetaResponse page
) {
}
