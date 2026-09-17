package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.OperationId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
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
        /** Display name of the user who performed the operation. */
        String executedByName,
        LocalDateTime executedAt,
        SaleId saleId,
        /** Groups movements belonging to the same business operation. */
        OperationId operationId,
        /**
         * Remaining balance of the originating sale, or null for movements unrelated to a sale.
         * Zero indicates a settled sale.
         */
        Money saleAmountDue
) {
}
