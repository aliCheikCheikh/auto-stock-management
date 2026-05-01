package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ReceivingServiceTest {

    private StorageLocationRepository storageLocationRepository;
    private ReceivingService receivingService;

    @BeforeEach
    public void setUp() {
        storageLocationRepository = mock(StorageLocationRepository.class);
        receivingService = new ReceivingService(storageLocationRepository);
    }

    @Test
    public void should_receive_stock_and_create_movements() {
        // GIVEN
        ProductId productId = ProductId.generate();
        UserId userId = UserId.generate();
        ShopId shopId = ShopId.generate();

        StorageLocation shopFloor = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Shop Floor", 3);
        StorageLocation backStock = new StorageLocation(LocationId.generate(), shopId, LocationType.BACKSTOCK, "Backstock", 3);

        // Entries requested by the user
        List<ReceivingEntry> entries = List.of(
                ReceivingEntry.of(productId, shopFloor.getLocationId(), 15),
                ReceivingEntry.of(productId, backStock.getLocationId(), 35)
        );

        // Stub the repository
        when(storageLocationRepository.findById(shopFloor.getLocationId())).thenReturn(Optional.of(shopFloor));
        when(storageLocationRepository.findById(backStock.getLocationId())).thenReturn(Optional.of(backStock));

        // WHEN
        List<StockMovement> movements = receivingService.receive(entries, userId);

        // THEN
        // 1. Verify StorageLocations were modified in memory
        assertThat(shopFloor.getStockLevel(productId)).isEqualTo(15);
        assertThat(backStock.getStockLevel(productId)).isEqualTo(35);

        // 2. Verify StorageLocations were explicitly saved
        verify(storageLocationRepository, times(1)).save(shopFloor);
        verify(storageLocationRepository, times(1)).save(backStock);

        // 3. Verify movements were created correctly
        assertThat(movements).hasSize(2);

        // --- Verify movement for SHOP_FLOOR ---
        StockMovement shopFloorMovement = movements.stream()
                .filter(m -> m.getDestinationLocationId().isPresent() &&
                        m.getDestinationLocationId().get().equals(shopFloor.getLocationId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Movement for shop floor not found"));

        assertThat(shopFloorMovement.getQuantity()).isEqualTo(15);
        assertThat(shopFloorMovement.getProductId()).isEqualTo(productId);
        assertThat(shopFloorMovement.getSourceLocationId()).isEmpty();

        // --- Verify movement for BACKSTOCK ---
        StockMovement backStockMovement = movements.stream()
                .filter(m -> m.getDestinationLocationId().isPresent() &&
                        m.getDestinationLocationId().get().equals(backStock.getLocationId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Movement for back stock not found"));

        assertThat(backStockMovement.getQuantity()).isEqualTo(35);
        assertThat(backStockMovement.getProductId()).isEqualTo(productId);
        assertThat(backStockMovement.getSourceLocationId()).isEmpty();
    }
}