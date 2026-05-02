package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.SellLineCommand;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.event.LowStockAlert;
import com.aliCheikh.stock.domain.event.SaleCompleted;
import com.aliCheikh.stock.domain.event.ShopFloorLow;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.service.AllocationResult;
import com.aliCheikh.stock.domain.service.StockAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SellProductUseCaseTest {

    private StockAllocationService stockAllocationService;
    private StorageLocationRepository storageLocationRepository;
    private SaleRepository saleRepository;
    private StockMovementRepository stockMovementRepository;
    private ProductRepository productRepository;
    private EventPublisher eventPublisher;

    private SellProductUseCase sellProductUseCase;

    private SellProductCommand singleLineCommand;
    private StorageLocation singleLocation;
    private StorageLocation shopFloor;
    private StorageLocation backStock;
    private Product product;

    private ProductId productId;
    private ShopId shopId;
    private UserId sellerId;

    @BeforeEach
    public void setUp() {
        stockAllocationService = mock(StockAllocationService.class);
        storageLocationRepository = mock(StorageLocationRepository.class);
        saleRepository = mock(SaleRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        productRepository = mock(ProductRepository.class);
        eventPublisher = mock(EventPublisher.class);

        sellProductUseCase = new SellProductUseCase(
                stockAllocationService, storageLocationRepository, saleRepository,
                stockMovementRepository, productRepository, eventPublisher
        );

        productId = ProductId.generate();
        shopId = ShopId.generate();
        sellerId = UserId.generate();

        // New Command format with lines
        singleLineCommand = new SellProductCommand(sellerId, shopId, List.of(
                new SellLineCommand(productId, 4)
        ));

        // Setup storage locations with a low stock indicator of 3
        shopFloor = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Main Shop Floor", 3);
        singleLocation = new StorageLocation(LocationId.generate(), shopId, LocationType.SHOP_FLOOR, "Storage", 3);
        backStock = new StorageLocation(LocationId.generate(), shopId, LocationType.BACKSTOCK, "Back Stock", 3);

        shopFloor.increaseStock(productId, 3);
        singleLocation.increaseStock(productId, 6);
        backStock.increaseStock(productId, 7);

        Money price = Money.create(BigDecimal.valueOf(15), Currency.getInstance("EUR"));
        product = new Product(productId, "Oil Filter", "REF-123", CategoryId.generate(), 5, price);
    }

    @Test
    public void should_orchestrate_sale_on_single_location_when_stock_is_sufficient() {
        // GIVEN
        when(stockAllocationService.allocate(productId, 4, shopId))
                .thenReturn(List.of(AllocationResult.of(singleLocation.getLocationId(), 4)));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        // We only mock findByShopId now, because findById is removed for locations!
        when(storageLocationRepository.findByShopId(shopId)).thenReturn(List.of(singleLocation));

        // WHEN
        sellProductUseCase.sell(singleLineCommand);

        // THEN
        verify(saleRepository, times(1)).save(any(Sale.class));

        // Verifying the new Batch Saves
        verify(storageLocationRepository, times(1)).saveAll(anyList());
        verify(stockMovementRepository, times(1)).saveAll(anyList());

        verify(eventPublisher, times(1)).publish(anyList());
    }

    @Test
    public void should_orchestrate_sale_and_split_stock_across_multiple_locations() {
        // GIVEN
        when(stockAllocationService.allocate(productId, 4, shopId))
                .thenReturn(List.of(
                        AllocationResult.of(shopFloor.getLocationId(), 3),
                        AllocationResult.of(backStock.getLocationId(), 1)
                ));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(storageLocationRepository.findByShopId(shopId)).thenReturn(List.of(shopFloor, backStock));

        // WHEN
        sellProductUseCase.sell(singleLineCommand);

        // THEN
        verify(storageLocationRepository, times(1)).saveAll(anyList()); // Called once for all locations
        verify(stockMovementRepository, times(1)).saveAll(anyList()); // Called once for all movements
        verify(saleRepository, times(1)).save(any(Sale.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());

        List<DomainEvent> publishedEvents = eventCaptor.getValue();
        assertThat(publishedEvents).hasSize(2);

        ShopFloorLow shopAlert = (ShopFloorLow) publishedEvents.stream()
                .filter(e -> e instanceof ShopFloorLow).findFirst().orElseThrow();
        assertThat(shopAlert.locationId()).isEqualTo(shopFloor.getLocationId());
        assertThat(shopAlert.currentQuantity()).isEqualTo(0);

        SaleCompleted saleEvent = (SaleCompleted) publishedEvents.stream()
                .filter(e -> e instanceof SaleCompleted).findFirst().orElseThrow();
        assertThat(saleEvent.lineItemCount()).isEqualTo(1); // 1 product sold
    }

    @Test
    public void should_emit_all_alerts_when_critical_thresholds_are_reached() {
        // GIVEN
        SellProductCommand largeCommand = new SellProductCommand(sellerId, shopId, List.of(
                new SellLineCommand(productId, 6)
        ));

        when(stockAllocationService.allocate(productId, 6, shopId))
                .thenReturn(List.of(
                        AllocationResult.of(shopFloor.getLocationId(), 3),
                        AllocationResult.of(backStock.getLocationId(), 3)
                ));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(storageLocationRepository.findByShopId(shopId)).thenReturn(List.of(shopFloor, backStock));

        // WHEN
        sellProductUseCase.sell(largeCommand);

        // THEN
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());

        List<DomainEvent> publishedEvents = eventCaptor.getValue();
        assertThat(publishedEvents).hasSize(3); // SaleCompleted + ShopFloorLow + LowStockAlert

        LowStockAlert globalAlert = (LowStockAlert) publishedEvents.stream()
                .filter(e -> e instanceof LowStockAlert).findFirst().orElseThrow();
        assertThat(globalAlert.globalQuantity()).isEqualTo(4); // (3+7) initial - 6 sold
    }

    // --- NEW TEST: Multi-line support ---
    @Test
    public void should_process_multi_line_sale_correctly() {
        // GIVEN
        ProductId secondProductId = ProductId.generate();
        Product secondProduct = new Product(secondProductId, "Spark Plug", "REF-456", CategoryId.generate(), 10, Money.create(new BigDecimal("5.0"), Currency.getInstance("EUR")));

        singleLocation.increaseStock(secondProductId, 10); // Adding stock for second product

        SellProductCommand multiLineCommand = new SellProductCommand(sellerId, shopId, List.of(
                new SellLineCommand(productId, 2),
                new SellLineCommand(secondProductId, 5)
        ));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findById(secondProductId)).thenReturn(Optional.of(secondProduct));

        when(stockAllocationService.allocate(productId, 2, shopId))
                .thenReturn(List.of(AllocationResult.of(singleLocation.getLocationId(), 2)));
        when(stockAllocationService.allocate(secondProductId, 5, shopId))
                .thenReturn(List.of(AllocationResult.of(singleLocation.getLocationId(), 5)));

        when(storageLocationRepository.findByShopId(shopId)).thenReturn(List.of(singleLocation));

        // WHEN
        sellProductUseCase.sell(multiLineCommand);

        // THEN
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher, times(1)).publish(eventCaptor.capture());

        List<DomainEvent> publishedEvents = eventCaptor.getValue();

        SaleCompleted saleEvent = (SaleCompleted) publishedEvents.stream()
                .filter(e -> e instanceof SaleCompleted).findFirst().orElseThrow();

        // Validating that the sale successfully captured BOTH lines!
        assertThat(saleEvent.lineItemCount()).isEqualTo(2);
    }
}