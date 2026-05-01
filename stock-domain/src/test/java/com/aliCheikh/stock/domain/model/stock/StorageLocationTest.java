package com.aliCheikh.stock.domain.model.stock;

import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.event.ShopFloorLow;
import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StorageLocationTest {

    private LocationId locationId;
    private ShopId shopId;
    private ProductId productId;
    private LocationType locationType;
    private String label;
    private int lowStockIndicator;
    private StorageLocation storageLocation;

    @BeforeEach
    public void setup() {
        locationId = LocationId.of(UUID.randomUUID());
        shopId = ShopId.of(UUID.randomUUID());
        locationType = LocationType.SHOP_FLOOR;
        label = "Main Shop";
        lowStockIndicator = 3;
        productId = ProductId.generate();
        storageLocation = new StorageLocation(locationId, shopId, locationType, label, lowStockIndicator);
    }

    // --- PRE-EXISTING CORE TESTS ---

    @Test
    public void should_create_stock_level_when_product_not_exists_and_increase_stock_quantity() {
        int quantityToIncrease = 5;
        storageLocation.increaseStock(productId, quantityToIncrease);
        assertThat(storageLocation.getStockLevel(productId)).isEqualTo(quantityToIncrease);
    }

    @Test
    public void should_throw_exception_when_decreasing_more_than_available_stock() {
        int quantityToIncrease = 5;
        int quantityToDecrease = 10;
        storageLocation.increaseStock(productId, quantityToIncrease);
        assertThatThrownBy(() -> storageLocation.decreaseStock(productId, quantityToDecrease))
                .isInstanceOf(InsufficientStockException.class)
                .extracting(ex -> ((InsufficientStockException) ex)).satisfies(ex -> {
                    assertThat(ex.getProductId()).isEqualTo(productId);
                    assertThat(ex.getAvailableQuantity()).isEqualTo(quantityToIncrease);
                    assertThat(ex.getRequestedQuantity()).isEqualTo(quantityToDecrease);
                });
    }

    @Test
    public void should_decrease_stock_when_enough_quantity() {
        int quantityToIncrease = 10;
        int quantityToDecrease = 5;
        storageLocation.increaseStock(productId, quantityToIncrease);
        storageLocation.decreaseStock(productId, quantityToDecrease);
        assertThat(storageLocation.getStockLevel(productId)).isEqualTo(quantityToDecrease);
    }

    @Test
    public void should_throw_exception_when_decreasing_stock_for_uninitialized_product() {
        // GIVEN: A product that never had an increaseStock operation
        int quantityToDecrease = 5;
        assertThatThrownBy(() -> storageLocation.decreaseStock(productId, quantityToDecrease))
                .isInstanceOf(InsufficientStockException.class)
                .extracting(ex -> ((InsufficientStockException) ex)).satisfies(ex -> {
                    assertThat(ex.getProductId()).isEqualTo(productId);
                    assertThat(ex.getAvailableQuantity()).isEqualTo(0);
                    assertThat(ex.getRequestedQuantity()).isEqualTo(quantityToDecrease);
                });
    }

    @Test
    public void should_evaluate_shop_floor_low_indicator_correctly() {
        // 1. TRUE Case: Shop Floor AND stock is below threshold (which is 3)
        storageLocation.increaseStock(productId, 2);
        assertThat(storageLocation.isShopFloorLow(productId)).isTrue();

        // 2. FALSE Case: Shop Floor BUT stock is sufficient
        storageLocation.increaseStock(productId, 2); // Stock becomes 4
        assertThat(storageLocation.isShopFloorLow(productId)).isFalse();

        // 3. FALSE Case: Not a Shop Floor (BACKSTOCK)
        StorageLocation backStock = new StorageLocation(LocationId.generate(), shopId, LocationType.BACKSTOCK, "Back Stock", 3);
        backStock.increaseStock(productId, 1); // Stock is low (1 < 3)
        assertThat(backStock.isShopFloorLow(productId)).isFalse(); // BUT it is a BACKSTOCK!
    }

    // --- NEW EVENT-DRIVEN TESTS (Phase 2 Refactoring) ---

    @Test
    public void should_emit_ShopFloorLow_event_when_decrease_stock_falls_below_indicator() {
        // GIVEN: Initial stock is 10 (indicator is 3)
        storageLocation.increaseStock(productId, 10);

        // WHEN: We remove 8 units (Remaining is 2, which is < 3)
        storageLocation.decreaseStock(productId, 8);

        // THEN: The event must be recorded in the aggregate
        List<DomainEvent> events = storageLocation.pullEvents();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ShopFloorLow.class);

        ShopFloorLow event = (ShopFloorLow) events.get(0);
        assertThat(event.productId()).isEqualTo(productId);
        assertThat(event.locationId()).isEqualTo(locationId);
        assertThat(event.currentQuantity()).isEqualTo(2);
        assertThat(event.lowStockIndicator()).isEqualTo(3);
    }

    @Test
    public void should_not_emit_event_when_decrease_stock_remains_above_indicator() {
        // GIVEN: Initial stock is 10
        storageLocation.increaseStock(productId, 10);

        // WHEN: We remove 2 units (Remaining is 8, which is >= 3)
        storageLocation.decreaseStock(productId, 2);

        // THEN: No event should be emitted
        assertThat(storageLocation.pullEvents()).isEmpty();
    }

    @Test
    public void pullEvents_should_clear_internal_events_list() {
        // GIVEN
        storageLocation.increaseStock(productId, 5);
        storageLocation.decreaseStock(productId, 4); // Remaining 1, triggers event

        // WHEN
        storageLocation.pullEvents(); // First call captures the event

        // THEN
        assertThat(storageLocation.pullEvents()).isEmpty(); // Second call must be empty
    }
}