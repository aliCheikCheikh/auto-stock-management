package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.application.dto.TransferStockCommand;

import java.util.Objects;

/**
 * Use Case for transferring stock internally between two storage locations.
 * Modifies the stock levels and generates a TRANSFER movement.
 * Does not emit global stock events as the total stock remains unchanged.
 */
public class TransferStockUseCase {

    private final StorageLocationRepository storageLocationRepository;
    private final StockMovementRepository stockMovementRepository;
    private final EventPublisher eventPublisher; // Injecté selon ta spec, même s'il n'émet rien pour le moment.

    public TransferStockUseCase(
            StorageLocationRepository storageLocationRepository,
            StockMovementRepository stockMovementRepository,
            EventPublisher eventPublisher) {
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository);
        this.stockMovementRepository = Objects.requireNonNull(stockMovementRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    public void execute(TransferStockCommand command) {

        // 1. Récupération des agrégats
        StorageLocation source = storageLocationRepository.findById(command.sourceLocationId()).orElseThrow(()-> new StorageNotFoundException(command.sourceLocationId()));
        StorageLocation destination = storageLocationRepository.findById(command.destinationLocationId()).orElseThrow(()-> new StorageNotFoundException(command.destinationLocationId()));

        // PHASE 1 : Validation (L'invariant est protégé )
        if (!source.hasEnoughStock(command.productId(), command.quantity())) {
            throw new InsufficientStockException(command.productId(), source.getStockLevel(command.productId()), command.quantity());
        }

        // PHASE 2 : Exécution (Modification de l'état en mémoire)
        source.decreaseStock(command.productId(), command.quantity());
        destination.increaseStock(command.productId(), command.quantity());

        // Sauvegarde explicite des modifications
        storageLocationRepository.save(source);
        storageLocationRepository.save(destination);

        // PHASE 3 : Enregistrement (Génération de la trace)
        StockMovement transferMovement = StockMovement.createTransfer(
                command.productId(),
                command.sourceLocationId(),
                command.destinationLocationId(),
                command.quantity(),
                command.userId()
        );

        stockMovementRepository.save(transferMovement);
    }
}