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

        // All movements in one receipt share an operation ID, including receipts with multiple
        // products.
        OperationId operationId = OperationId.generate();

        for (ReceivingEntry entry : entries) {

            StorageLocation location = storageLocationRepository.findById(entry.locationId()).orElseThrow(() -> new StorageNotFoundException(entry.locationId()));

            location.increaseStock(entry.productId(), entry.quantity());

            storageLocationRepository.save(location);

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

        return generatedMovements;
    }
}
