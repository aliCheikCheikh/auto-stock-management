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

    /** Opération métier à l'origine de ce mouvement : réception, transfert ou vente. */
    private final OperationId operationId;


    private StockMovement(MovementId movementId,
                          ProductId productId,
                          LocationId sourceLocationId,
                          LocationId destinationLocationId,
                          MovementType movementType,
                          int quantity,
                          UserId performedBy,
                          LocalDateTime occurredAt,
                          SaleId saleId,
            OperationId operationId) {
        this.movementId = movementId;
        this.productId = productId;
        this.sourceLocationId = sourceLocationId;
        this.destinationLocationId = destinationLocationId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.performedBy = performedBy;
        this.occurredAt = occurredAt;
        this.saleId = saleId;
        this.operationId = Objects.requireNonNull(operationId, "operationId cannot be null");

    }

    public static StockMovement createEntry(ProductId productId, LocationId destinationLocationId, int quantity, UserId userId, OperationId operationId) {

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
                null,
                operationId);

    }


    public static StockMovement createExit(ProductId productId, LocationId sourceLocationId, int quantity, UserId userId, SaleId saleId, OperationId operationId) {

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
                saleId,
                operationId);

    }

    public static StockMovement createTransfer(ProductId productId, LocationId sourceLocationId, LocationId destinationLocationId, int quantity, UserId userId, OperationId operationId) {

        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        StockMovement.requiredPositiveQuantity(quantity);

        if (destinationLocationId == null) {
            throw new InvalidMovementException(MovementErrorReason.ENTRY_MISSING_DESTINATION, "destinationLocationId cannot be null");
        }

        if (sourceLocationId == null) {
            throw new InvalidMovementException(MovementErrorReason.EXIT_MISSING_SOURCE, "sourceLocationId cannot be null");
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
                null,
                operationId);

    }

    private static void requiredPositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityMovementException(quantity);
        }
    }

    public static StockMovement rehydrate(
            MovementId movementId,
            ProductId productId,
            LocationId sourceLocationId,
            LocationId destinationLocationId,
            MovementType movementType,
            int quantity,
            UserId performedBy,
            LocalDateTime occurredAt,
            SaleId saleId,
            OperationId operationId
    ) {
        Objects.requireNonNull(movementId, "movementId cannot be null");
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(movementType, "movementType cannot be null");
        Objects.requireNonNull(performedBy, "performedBy cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        requiredPositiveQuantity(quantity);

        validateMovementShape(movementType, sourceLocationId, destinationLocationId, saleId);

        return new StockMovement(
                movementId,
                productId,
                sourceLocationId,
                destinationLocationId,
                movementType,
                quantity,
                performedBy,
                occurredAt,
                saleId,
                operationId
        );
    }

    private static void validateMovementShape(
            MovementType movementType,
            LocationId sourceLocationId,
            LocationId destinationLocationId,
            SaleId saleId
    ) {
        switch (movementType) {
            case ENTRY -> {
                if (sourceLocationId != null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.ENTRY_MISSING_DESTINATION,
                            "entry movement cannot have a sourceLocationId"
                    );
                }
                if (destinationLocationId == null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.ENTRY_MISSING_DESTINATION,
                            "entry movement must have a destinationLocationId"
                    );
                }
                if (saleId != null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.ENTRY_MISSING_DESTINATION,
                            "entry movement cannot be linked to a sale"
                    );
                }
            }
            case EXIT -> {
                if (sourceLocationId == null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.EXIT_MISSING_SOURCE,
                            "exit movement must have a sourceLocationId"
                    );
                }
                if (destinationLocationId != null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.EXIT_MISSING_SOURCE,
                            "exit movement cannot have a destinationLocationId"
                    );
                }
                if (saleId == null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.EXIT_MISSING_SOURCE,
                            "exit movement must be linked to a sale"
                    );
                }
            }
            case TRANSFER -> {
                if (sourceLocationId == null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.EXIT_MISSING_SOURCE,
                            "transfer movement must have a sourceLocationId"
                    );
                }
                if (destinationLocationId == null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.ENTRY_MISSING_DESTINATION,
                            "transfer movement must have a destinationLocationId"
                    );
                }
                if (sourceLocationId.equals(destinationLocationId)) {
                    throw new InvalidMovementException(
                            MovementErrorReason.TRANSFER_SAME_SOURCE_DESTINATION,
                            "sourceLocationId and destinationLocationId cannot be the same"
                    );
                }
                if (saleId != null) {
                    throw new InvalidMovementException(
                            MovementErrorReason.TRANSFER_SAME_SOURCE_DESTINATION,
                            "transfer movement cannot be linked to a sale"
                    );
                }
            }
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

    /** L'opération à l'origine de ce mouvement. Tous les mouvements d'une même opération la partagent. */
    public OperationId getOperationId() {
        return operationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StockMovement that = (StockMovement) o;
        return that.movementId.equals(movementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movementId);
    }


}





