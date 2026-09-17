package com.aliCheikh.stock.infrastructure.persistence.entity;

import com.aliCheikh.stock.domain.model.movement.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "stock_movement")
public class StockMovementJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "source_location_id")
    private UUID sourceLocationId;

    @Column(name = "destination_location_id")
    private UUID destinationLocationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "sale_id")
    private UUID saleId;

    /** Business operation ID used to group movement history. */
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    protected StockMovementJpaEntity() {
    }

    private StockMovementJpaEntity(
            UUID id,
            UUID productId,
            UUID sourceLocationId,
            UUID destinationLocationId,
            MovementType movementType,
            int quantity,
            UUID performedBy,
            LocalDateTime occurredAt,
            UUID saleId,
            UUID operationId
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.movementType = Objects.requireNonNull(movementType, "movementType cannot be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }

        this.quantity = quantity;
        this.performedBy = Objects.requireNonNull(performedBy, "performedBy cannot be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        this.saleId = saleId;
        this.operationId = Objects.requireNonNull(operationId, "operationId cannot be null");
    }

    public static StockMovementJpaEntity of(
            UUID id,
            UUID productId,
            UUID sourceLocationId,
            UUID destinationLocationId,
            MovementType movementType,
            int quantity,
            UUID performedBy,
            LocalDateTime occurredAt,
            UUID saleId,
            UUID operationId
    ) {
        return new StockMovementJpaEntity(
                id,
                productId,
                sourceLocationId,
                destinationLocationId,
                movementType,
                quantity,
                performedBy,
                occurredAt,
                saleId,
                operationId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getSourceLocationId() {
        return sourceLocationId;
    }

    public UUID getDestinationLocationId() {
        return destinationLocationId;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public UUID getSaleId() {
        return saleId;
    }
}
