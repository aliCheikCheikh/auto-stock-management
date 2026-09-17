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

        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("requestedQuantity must be strictly positive");
        }

        List<StorageLocation> locations = repository.findByShopId(shopId);
        if (locations == null || locations.isEmpty()) {
            // If no locations exist, available stock is effectively 0
            throw new InsufficientStockException(productId, 0, requestedQuantity);
        }

        int totalAvailableStock = locations.stream()
                .mapToInt(loc -> loc.getStockLevel(productId))
                .sum();

        if (totalAvailableStock < requestedQuantity) {
            throw new InsufficientStockException(productId, totalAvailableStock, requestedQuantity);
        }

        // Allocate from the shop floor before backstock.
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
                break; // Allocation fulfilled
            }
        }


        return allocations;
    }
}
