package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.domain.model.movement.MovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID movementId,
        UUID productId,
        UUID locationId,
        UUID destinationLocationId,
        MovementType type,
        int quantity,
        UUID executedBy,
        String executedByName,
        LocalDateTime executedAt,
        UUID saleId,
        UUID operationId,
        /**
         * Solde restant dû de la vente à l'origine du mouvement. {@code null} hors vente, zéro
         * lorsque la vente est réglée : le front en déduit « payée » ou « à crédit — reste X »
         * sans refaire le moindre calcul.
         */
        MoneyResponse saleAmountDue
) {
}
