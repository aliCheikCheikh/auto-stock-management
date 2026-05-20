package com.aliCheikh.stock.infrastructure.persistence.projection;

import com.aliCheikh.stock.domain.model.stock.LocationType;

import java.util.UUID;

public record StockLevelRow(UUID productId,
                            String productName,
                            UUID locationId,
                            String locationName,
                            LocationType locationType,
                            UUID shopId,
                            int quantity) {
}
