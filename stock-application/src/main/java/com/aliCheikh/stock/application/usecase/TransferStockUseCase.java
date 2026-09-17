package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.TransferStockCommand;
import com.aliCheikh.stock.application.dto.TransferStockResult;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.product.InactiveProductException;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferReason;
import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.model.movement.OperationId;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Use case for transferring stock from a backstock location to a shop floor location.
 *
 * <p>This use case represents the operational replenishment flow used by the shop:
 * stock is moved from the reserve area ({@link LocationType#BACKSTOCK}) to the
 * customer-facing area ({@link LocationType#SHOP_FLOOR}) within the same shop.</p>
 *
 * <p>Business rules enforced by this use case:</p>
 * <ul>
 *     <li>source and destination locations must exist;</li>
 *     <li>source and destination must belong to the same shop;</li>
 *     <li>the source location must be {@code BACKSTOCK};</li>
 *     <li>the destination location must be {@code SHOP_FLOOR};</li>
 *     <li>the source location must contain enough stock for the requested product;</li>
 *     <li>one immutable {@code TRANSFER} stock movement is recorded.</li>
 * </ul>
 *
 * <p>The global stock for the shop does not change during a transfer, so this use case
 * does not publish global stock events. Transaction management is handled by the
 * infrastructure layer.</p>
 */
public class TransferStockUseCase {

    private final StorageLocationRepository storageLocationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public TransferStockUseCase(
            StorageLocationRepository storageLocationRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            EventPublisher eventPublisher,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository, "storageLocationRepository cannot be null");
        this.stockMovementRepository = Objects.requireNonNull(stockMovementRepository, "stockMovementRepository cannot be null");
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }


    /**
     * Executes a stock transfer according to the reserve-to-shop-floor contract.
     *
     * @param command validated transfer request
     * @throws ProductNotFoundException      if the product does not exist
     * @throws InactiveProductException      if the product is inactive
     * @throws StorageNotFoundException      if source or destination does not exist
     * @throws InvalidStockTransferException if locations do not satisfy transfer rules
     * @throws InsufficientStockException    if the source does not have enough stock
     */
    public TransferStockResult execute(TransferStockCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        return transactionRunner.execute(() -> doExecute(command));
    }

    private TransferStockResult doExecute(TransferStockCommand command) {
        Instant acceptedAt = clock.instant();
        LocalDateTime occurredAt = LocalDateTime.ofInstant(acceptedAt, clock.getZone());
        Product product = productRepository
                .findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        product.ensureActive();

        StorageLocation source = storageLocationRepository.findById(command.sourceLocationId())
                .orElseThrow(() -> new StorageNotFoundException(command.sourceLocationId()));

        StorageLocation destination = storageLocationRepository.findById(command.destinationLocationId())
                .orElseThrow(() -> new StorageNotFoundException(command.destinationLocationId()));

        validateTransfer(source, destination);

        if (!source.hasEnoughStock(command.productId(), command.quantity())) {
            throw new InsufficientStockException(
                    command.productId(),
                    source.getStockLevel(command.productId()),
                    command.quantity()
            );
        }

        source.decreaseStock(command.productId(), command.quantity(), occurredAt);
        destination.increaseStock(command.productId(), command.quantity());

        storageLocationRepository.save(source);
        storageLocationRepository.save(destination);

        StockMovement transferMovement = StockMovement.createTransfer(
                command.productId(),
                command.sourceLocationId(),
                command.destinationLocationId(),
                command.quantity(),
                command.userId(),
                // Assign an operation ID to keep transfer history consistent with other movements.
                OperationId.generate(),
                occurredAt
        );

        stockMovementRepository.save(transferMovement);

        return new TransferStockResult(
                transferMovement.getMovementId(),
                acceptedAt
        );
    }

    private void validateTransfer(StorageLocation source, StorageLocation destination) {
        if (!source.getShopId().equals(destination.getShopId())) {
            throw new InvalidStockTransferException(
                    source.getLocationId(),
                    destination.getLocationId(),
                    InvalidStockTransferReason.CROSS_SHOP_TRANSFER
            );
        }

        if (source.getLocationType() != LocationType.BACKSTOCK
                || destination.getLocationType() != LocationType.SHOP_FLOOR) {
            throw new InvalidStockTransferException(
                    source.getLocationId(),
                    destination.getLocationId(),
                    InvalidStockTransferReason.INVALID_TRANSFER_DIRECTION
            );
        }
    }
}
