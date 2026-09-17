package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ReceivingServiceTest {

    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.of(2026, 8, 5, 11, 15, 30);

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
        List<StockMovement> movements = receivingService.receive(entries, userId, BUSINESS_TIME);

        // THEN
        // 1. Verify StorageLocations were modified in memory
        assertThat(shopFloor.getStockLevel(productId)).isEqualTo(15);
        assertThat(backStock.getStockLevel(productId)).isEqualTo(35);

        // 2. Verify StorageLocations were explicitly saved
        verify(storageLocationRepository, times(1)).save(shopFloor);
        verify(storageLocationRepository, times(1)).save(backStock);

        // 3. Verify movements were created correctly
        assertThat(movements).hasSize(2);
        assertThat(movements).extracting(StockMovement::getOccurredAt).containsOnly(BUSINESS_TIME);

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

    @Test
    public void all_movements_of_one_reception_share_the_same_operation() {
        ProductId productId = ProductId.generate();
        UserId userId = UserId.generate();
        ShopId shopId = ShopId.generate();

        StorageLocation shopFloor = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Shop Floor", 3);
        StorageLocation backStock = new StorageLocation(LocationId.generate(), shopId, LocationType.BACKSTOCK, "Backstock", 3);

        when(storageLocationRepository.findById(shopFloor.getLocationId())).thenReturn(Optional.of(shopFloor));
        when(storageLocationRepository.findById(backStock.getLocationId())).thenReturn(Optional.of(backStock));

        List<StockMovement> movements = receivingService.receive(List.of(
                ReceivingEntry.of(productId, shopFloor.getLocationId(), 15),
                ReceivingEntry.of(productId, backStock.getLocationId(), 35)
        ), userId, BUSINESS_TIME);

        // One receipt must share a single operation ID.
        assertThat(movements).hasSize(2);
        assertThat(movements).extracting(StockMovement::getOperationId).containsOnly(movements.get(0).getOperationId());
    }

    @Test
    public void two_receptions_do_not_share_their_operation() {
        ProductId productId = ProductId.generate();
        UserId userId = UserId.generate();
        ShopId shopId = ShopId.generate();

        StorageLocation shopFloor = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Shop Floor", 3);
        when(storageLocationRepository.findById(shopFloor.getLocationId())).thenReturn(Optional.of(shopFloor));

        List<ReceivingEntry> entries = List.of(ReceivingEntry.of(productId, shopFloor.getLocationId(), 5));

        // Successive receipts by the same user at the same location remain separate operations.
        StockMovement first = receivingService.receive(entries, userId, BUSINESS_TIME).get(0);
        StockMovement second = receivingService.receive(entries, userId, BUSINESS_TIME).get(0);

        assertThat(first.getOperationId()).isNotEqualTo(second.getOperationId());
    }
}
