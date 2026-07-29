package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.OperationId;
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
        /** Nom affichable de l'auteur : « c'est Ahmat qui a fait cette réception ». */
        String executedByName,
        LocalDateTime executedAt,
        SaleId saleId,
        /** Opération à l'origine du mouvement : les lignes qui la partagent se regroupent. */
        OperationId operationId
) {
}
