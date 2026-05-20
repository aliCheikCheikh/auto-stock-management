package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.domain.model.stock.LocationType;

import java.util.UUID;

public record StockLevelResponse(UUID productId,
                                 String productName,
                                 UUID locationId,
                                 String locationName,
                                 LocationType locationType,
                                 UUID shopId,
                                 int quantity) {
}
