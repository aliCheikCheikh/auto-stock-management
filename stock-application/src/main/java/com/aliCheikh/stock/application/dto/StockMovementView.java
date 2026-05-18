package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;

public record StockMovementView(
        MovementId movementId,
        ProductId productId,
        LocationId locationId,
        LocationId destinationLocationId,
        MovementType type,
        int quantity,
        UserId executedBy,
        LocalDateTime executedAt,
        SaleId saleId
) {
}
