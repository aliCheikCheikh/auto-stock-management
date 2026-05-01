package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockAllocationServiceTest {

    private StockAllocationService service;
    private StorageLocationRepository repository;

    private ProductId productId;
    private ShopId shopId;

    private StorageLocation shopFloorPrimary;
    private StorageLocation shopFloorSecondary;
    private StorageLocation shopFloorLimited;

    private StorageLocation backstockPrimary;
    private StorageLocation backstockLimited;

    private int requestedQuantity;

    @BeforeEach
    void setUp() {
        requestedQuantity = 4;

        shopId = ShopId.generate();
        productId = ProductId.generate();

        shopFloorPrimary = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Main shop floor", 3);
        shopFloorSecondary = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Secondary shop floor", 3);
        shopFloorLimited = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Limited shop floor", 3);

        backstockPrimary = new StorageLocation(LocationId.generate(), shopId, LocationType.BACKSTOCK, "Main backstock", 3);
        backstockLimited = new StorageLocation(LocationId.generate(), shopId, LocationType.BACKSTOCK, "Limited backstock", 3);

        // stock setup
        shopFloorPrimary.increaseStock(productId, 10);
        shopFloorSecondary.increaseStock(productId, 2);
        shopFloorLimited.increaseStock(productId, 3);

        backstockPrimary.increaseStock(productId, 10);
        backstockLimited.increaseStock(productId, 1);

        repository = mock(StorageLocationRepository.class);
        service = new StockAllocationService(repository);
    }

    @Test
    void should_allocate_only_from_shop_floor_when_shop_floor_has_enough_stock() {
        // GIVEN
        when(repository.findByShopId(shopId)).thenReturn(List.of(shopFloorPrimary, backstockPrimary));

        // WHEN
        List<AllocationResult> result = service.allocate(productId, requestedQuantity, shopId);

        // THEN
        assertEquals(1, result.size());
        assertEquals(4, result.get(0).getQuantity());
        assertEquals(shopFloorPrimary.getLocationId(), result.get(0).getLocationId());
    }

    @Test
    void should_allocate_from_shop_floor_and_complete_from_backstock_when_shop_floor_is_insufficient() {
        // GIVEN
        when(repository.findByShopId(shopId)).thenReturn(List.of(shopFloorLimited, backstockPrimary));

        // WHEN
        List<AllocationResult> result = service.allocate(productId, requestedQuantity, shopId);

        // THEN
        assertEquals(2, result.size());

        assertTrue(result.stream().anyMatch(r -> r.getLocationId().equals(shopFloorLimited.getLocationId()) && r.getQuantity() == 3));

        assertTrue(result.stream().anyMatch(r -> r.getLocationId().equals(backstockPrimary.getLocationId()) && r.getQuantity() == 1));
    }

    @Test
    void should_throw_exception_when_total_stock_is_insufficient() {
        // GIVEN
        when(repository.findByShopId(shopId)).thenReturn(List.of(shopFloorLimited, backstockLimited));

        // WHEN & THEN
        assertThrows(InsufficientStockException.class, () -> service.allocate(productId, 10, shopId));
    }

    @Test
    void should_prioritize_shop_floor_over_backstock_even_if_input_order_is_unsorted() {
        // GIVEN
        when(repository.findByShopId(shopId)).thenReturn(List.of(backstockPrimary, shopFloorLimited));

        // WHEN
        List<AllocationResult> result = service.allocate(productId, requestedQuantity, shopId);

        // THEN
        assertEquals(shopFloorLimited.getLocationId(), result.get(0).getLocationId());
    }

    @Test
    void should_use_all_shop_floor_locations_before_using_backstock() {
        // GIVEN
        when(repository.findByShopId(shopId)).thenReturn(List.of(shopFloorSecondary, shopFloorLimited, backstockPrimary));

        // WHEN
        List<AllocationResult> result = service.allocate(productId, requestedQuantity, shopId);

        // THEN
        assertEquals(2, result.size());

        assertTrue(result.stream().anyMatch(r -> r.getLocationId().equals(shopFloorSecondary.getLocationId())));

        assertTrue(result.stream().anyMatch(r -> r.getLocationId().equals(shopFloorLimited.getLocationId())));

        assertFalse(result.stream().anyMatch(r -> r.getLocationId().equals(backstockPrimary.getLocationId())));
    }
}