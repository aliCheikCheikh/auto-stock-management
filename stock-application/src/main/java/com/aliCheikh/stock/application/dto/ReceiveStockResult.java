package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.product.ProductId;

import java.time.Instant;
import java.util.List;

public record ReceiveStockResult(ProductId productId,
                                 int totalReceived,
                                 List<MovementId> movementIds,
                                 Instant acceptedAt) {
}
