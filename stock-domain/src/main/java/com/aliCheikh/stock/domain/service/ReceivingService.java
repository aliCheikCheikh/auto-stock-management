package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReceivingService {

    private final StorageLocationRepository storageLocationRepository;

    public ReceivingService(StorageLocationRepository storageLocationRepository) {
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository, "storageLocationRepository cannot be null");
    }

    public List<StockMovement> receive(List<ReceivingEntry> entries, UserId userId) {
        List<StockMovement> generatedMovements = new ArrayList<>();

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
                    userId
            );

            generatedMovements.add(movement);
        }

        // 3. Return the created movements
        return generatedMovements;
    }
}