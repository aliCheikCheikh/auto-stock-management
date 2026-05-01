package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class StockAllocationService {

    private final StorageLocationRepository repository;

    public StockAllocationService(StorageLocationRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<AllocationResult> allocate(ProductId productId, int requestedQuantity, ShopId shopId) {

        // 1. Guard clauses
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("requestedQuantity must be strictly positive");
        }

        // 2. Récupérer les emplacements
        List<StorageLocation> locations = repository.findByShopId(shopId);
        if (locations == null || locations.isEmpty()) {
            // S'il n'y a pas d'emplacements, le stock dispo est de 0
            throw new InsufficientStockException(productId, 0, requestedQuantity);
        }

        // 3. Calcul du stock global pour le Fail Fast !
        int totalAvailableStock = locations.stream()
                .mapToInt(loc -> loc.getStockLevel(productId))
                .sum();

        if (totalAvailableStock < requestedQuantity) {
            // On lance l'exception riche !
            throw new InsufficientStockException(productId, totalAvailableStock, requestedQuantity);
        }

        // 4. On sait maintenant à 100% qu'on a assez de stock. On alloue.
        List<StorageLocation> sortedLocations = new ArrayList<>(locations);
        sortedLocations.sort(Comparator.comparing(loc -> loc.getLocationType().getPriority()));

        List<AllocationResult> allocations = new ArrayList<>();
        int remaining = requestedQuantity;

        for (StorageLocation location : sortedLocations) {
            int available = location.getStockLevel(productId);

            if (available <= 0) {
                continue;
            }

            int allocated = Math.min(available, remaining);
            allocations.add(AllocationResult.of(location.getLocationId(), allocated));
            remaining -= allocated;

            if (remaining == 0) {
                break; // Demande satisfaite
            }
        }

        // Plus besoin de la vérification finale (remaining > 0), le Fail Fast nous a garantis le succès.

        return allocations;
    }
}