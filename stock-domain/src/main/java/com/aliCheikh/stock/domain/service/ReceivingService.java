package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.model.movement.OperationId;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReceivingService {

    private final StorageLocationRepository storageLocationRepository;

    public ReceivingService(StorageLocationRepository storageLocationRepository) {
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository, "storageLocationRepository cannot be null");
    }

    public List<StockMovement> receive(List<ReceivingEntry> entries, UserId userId, LocalDateTime occurredAt) {
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        List<StockMovement> generatedMovements = new ArrayList<>();

        // Une réception est une opération unique, même lorsqu'elle porte sur plusieurs produits :
        // tous ses mouvements partagent la même identité pour être regroupés à la lecture.
        OperationId operationId = OperationId.generate();

        // 1. For each ReceivingEntry:
        for (ReceivingEntry entry : entries) {

            // a. Retrieve StorageLocation
            StorageLocation location = storageLocationRepository.findById(entry.locationId()).orElseThrow(() -> new StorageNotFoundException(entry.locationId()));

            // b. Increase the stock in memory
            location.increaseStock(entry.productId(), entry.quantity());

            // CORRECTION: Save the updated aggregate back to the repository
            storageLocationRepository.save(location);

            // c. Create the inbound StockMovement
            StockMovement movement = StockMovement.createEntry(
                    entry.productId(),
                    entry.locationId(),
                    entry.quantity(),
                    userId,
                    operationId,
                    occurredAt
            );

            generatedMovements.add(movement);
        }

        // 3. Return the created movements
        return generatedMovements;
    }
}
