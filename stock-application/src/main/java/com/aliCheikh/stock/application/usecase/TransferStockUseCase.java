package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.TransferStockCommand;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockTransferReason;
import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransferStockUseCase {

    private final StorageLocationRepository storageLocationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final EventPublisher eventPublisher;

    public TransferStockUseCase(
            StorageLocationRepository storageLocationRepository,
            StockMovementRepository stockMovementRepository,
            EventPublisher eventPublisher
    ) {
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository);
        this.stockMovementRepository = Objects.requireNonNull(stockMovementRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    public void execute(TransferStockCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

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

        source.decreaseStock(command.productId(), command.quantity());
        destination.increaseStock(command.productId(), command.quantity());

        storageLocationRepository.save(source);
        storageLocationRepository.save(destination);

        StockMovement transferMovement = StockMovement.createTransfer(
                command.productId(),
                command.sourceLocationId(),
                command.destinationLocationId(),
                command.quantity(),
                command.userId()
        );

        stockMovementRepository.save(transferMovement);
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