package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;

public record PageOfStockLevelResponse(List<StockLevelResponse> content,
                                       PageMetaResponse page) {
}
