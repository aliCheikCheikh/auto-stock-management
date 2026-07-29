package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.OperationId;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockMovementJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class StockMovementJpaMapper {

    public StockMovement toDomain(StockMovementJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return StockMovement.rehydrate(
                MovementId.of(entity.getId()),
                ProductId.of(entity.getProductId()),
                toLocationId(entity.getSourceLocationId()),
                toLocationId(entity.getDestinationLocationId()),
                entity.getMovementType(),
                entity.getQuantity(),
                UserId.of(entity.getPerformedBy()),
                entity.getOccurredAt(),
                toSaleId(entity.getSaleId()),
                OperationId.of(entity.getOperationId())
        );
    }

    public StockMovementJpaEntity toEntity(StockMovement movement) {
        Objects.requireNonNull(movement, "movement cannot be null");

        return StockMovementJpaEntity.of(
                movement.getMovementId().getValue(),
                movement.getProductId().getValue(),
                movement.getSourceLocationId()
                        .map(LocationId::getValue)
                        .orElse(null),
                movement.getDestinationLocationId()
                        .map(LocationId::getValue)
                        .orElse(null),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPerformedBy().getValue(),
                movement.getOccurredAt(),
                movement.getSaleId()
                        .map(SaleId::getValue)
                        .orElse(null),
                movement.getOperationId().getValue()
        );
    }

    private LocationId toLocationId(UUID value) {
        return value == null ? null : LocationId.of(value);
    }

    private SaleId toSaleId(UUID value) {
        return value == null ? null : SaleId.of(value);
    }
}
