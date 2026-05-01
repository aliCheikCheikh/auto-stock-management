package com.aliCheikh.stock.domain.model.movement;

import com.aliCheikh.stock.domain.exception.movement.InvalidMovementException;
import com.aliCheikh.stock.domain.exception.movement.InvalidQuantityMovementException;
import com.aliCheikh.stock.domain.exception.movement.MovementErrorReason;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;


public class StockMovement {
    private final MovementId movementId;
    private final ProductId productId;
    private final LocationId sourceLocationId;
    private final LocationId destinationLocationId;
    private final MovementType movementType;
    private final int quantity;
    private final UserId performedBy;
    private final LocalDateTime occurredAt;
    private final SaleId saleId;


    private StockMovement(MovementId movementId,
                          ProductId productId,
                          LocationId sourceLocationId,
                          LocationId destinationLocationId,
                          MovementType movementType,
                          int quantity,
                          UserId performedBy,
                          LocalDateTime occurredAt,
                          SaleId saleId) {
        this.movementId = movementId;
        this.productId = productId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.performedBy = performedBy;
        this.occurredAt = occurredAt;
        this.saleId = saleId;

    }

    public static StockMovement createEntry(ProductId productId, LocationId destinationLocationId, int quantity, UserId userId) {

        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        StockMovement.requiredPositiveQuantity(quantity);

        if (destinationLocationId == null) {
            throw new InvalidMovementException(MovementErrorReason.ENTRY_MISSING_DESTINATION, "destinationLocationId cannot be null");
        }

        return new StockMovement(MovementId.generate(),
                productId, null,
                destinationLocationId,
                MovementType.ENTRY,
                quantity, userId,
                LocalDateTime.now(),
                null);

    }


    public static StockMovement createExit(ProductId productId, LocationId sourceLocationId, int quantity, UserId userId, SaleId saleId) {

        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(saleId, "saleId cannot be null");
        StockMovement.requiredPositiveQuantity(quantity);

        if (sourceLocationId == null) {
            throw new InvalidMovementException(MovementErrorReason.EXIT_MISSING_SOURCE, "sourceLocationId cannot be null");
        }

        return new StockMovement(MovementId.generate(),
                productId,
                sourceLocationId,
                null,
                MovementType.EXIT,
                quantity,
                userId,
                LocalDateTime.now(),
                saleId);

    }

    public static StockMovement createTransfer(ProductId productId, LocationId sourceLocationId, LocationId destinationLocationId, int quantity, UserId userId) {

        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        StockMovement.requiredPositiveQuantity(quantity);

        if (destinationLocationId == null) {
            throw new IllegalArgumentException("destinationLocationId cannot be null");
        }

        if (sourceLocationId == null) {
            throw new IllegalArgumentException("sourceLocationId cannot be null");
        }


        if (sourceLocationId.equals(destinationLocationId)) {
            throw new InvalidMovementException(MovementErrorReason.TRANSFER_SAME_SOURCE_DESTINATION, "destinationLocationId cannot be the same as sourceLocationId");
        }

        return new StockMovement(MovementId.generate(),
                productId,
                sourceLocationId,
                destinationLocationId,
                MovementType.TRANSFER,
                quantity,
                userId,
                LocalDateTime.now(),
                null);

    }

    private static void requiredPositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityMovementException(quantity);
        }
    }

    public MovementId getMovementId() {
        return movementId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Optional<LocationId> getSourceLocationId() {
        return Optional.ofNullable(sourceLocationId);
    }

    public Optional<LocationId> getDestinationLocationId() {
        return Optional.ofNullable(destinationLocationId);
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public UserId getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public Optional<SaleId> getSaleId() {
        return Optional.ofNullable(saleId);
    }


}





