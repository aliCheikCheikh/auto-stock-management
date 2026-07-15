package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.SellLineCommand;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.event.LowStockAlert;
import com.aliCheikh.stock.domain.event.SaleCompleted;
import com.aliCheikh.stock.domain.event.ShopFloorLow;
import com.aliCheikh.stock.domain.exception.product.InactiveProductException;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
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
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SellProductUseCaseTest {

    private StockAllocationService stockAllocationService;
    private StorageLocationRepository storageLocationRepository;
    private SaleRepository saleRepository;
    private StockMovementRepository stockMovementRepository;
    private ProductRepository productRepository;
    private EventPublisher eventPublisher;
    private TransactionRunner transactionRunner;

    private SellProductUseCase sellProductUseCase;

    private ProductId productId;
    private ShopId shopId;
    private UserId sellerId;
    private Product product;

    @BeforeEach
    void setUp() {
        stockAllocationService = mock(StockAllocationService.class);
        storageLocationRepository = mock(StorageLocationRepository.class);
        saleRepository = mock(SaleRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        productRepository = mock(ProductRepository.class);
        eventPublisher = mock(EventPublisher.class);
        transactionRunner = new TransactionRunner() {
            @Override
            public <T> T execute(Supplier<T> work) {
                return work.get();
            }
        };

        sellProductUseCase = new SellProductUseCase(
                stockAllocationService,
                storageLocationRepository,
                saleRepository,
                stockMovementRepository,
                productRepository,
                eventPublisher,
                transactionRunner
        );

        productId = ProductId.generate();
        shopId = ShopId.generate();
        sellerId = UserId.generate();
        product = product(productId, "Oil Filter", "REF-123", 5, "15.00");
    }

    @Test
    void should_sell_single_line_from_one_location_and_publish_sale_completed() {
        StorageLocation location = storageLocation(LocationType.SHOP_FLOOR, "Shop floor", 3);
        location.increaseStock(productId, 10);

        SellProductCommand command = command(new SellLineCommand(productId, 4));

        givenProductExists(product);
        givenShopLocations(location);
        givenAllocation(productId, 4, location, 4);

        SellProductResult result = sellProductUseCase.sell(command);

        assertThat(location.getStockLevel(productId)).isEqualTo(6);

        Sale savedSale = captureSavedSale();
        assertThat(savedSale.getSoldBy()).isEqualTo(sellerId);
        assertThat(savedSale.getLines()).hasSize(1);
        assertThat(savedSale.getLines().get(0).productId()).isEqualTo(productId);
        assertThat(savedSale.getLines().get(0).quantity()).isEqualTo(4);

        List<StockMovement> movements = captureSavedMovements();
        assertThat(movements).hasSize(1);
        assertExitMovement(movements.get(0), productId, location.getLocationId(), 4, savedSale);

        List<DomainEvent> events = capturePublishedEvents();
        assertThat(events).anySatisfy(event -> {
            assertThat(event).isInstanceOf(SaleCompleted.class);
            SaleCompleted saleCompleted = (SaleCompleted) event;
            assertThat(saleCompleted.saleId()).isEqualTo(savedSale.getSaleId());
            assertThat(saleCompleted.lineItemCount()).isEqualTo(1);
            assertThat(saleCompleted.soldBy()).isEqualTo(sellerId);
        });
        assertThat(events).noneMatch(LowStockAlert.class::isInstance);
        assertThat(events).noneMatch(ShopFloorLow.class::isInstance);

        assertPersistenceHappensBeforePublication();
        assertSellProductResult(result, savedSale);
    }

    @Test
    void should_process_multi_line_sale_and_record_one_exit_movement_per_line() {
        ProductId secondProductId = ProductId.generate();
        Product secondProduct = product(secondProductId, "Spark Plug", "REF-456", 3, "5.00");

        StorageLocation location = storageLocation(LocationType.SHOP_FLOOR, "Shop floor", 3);
        location.increaseStock(productId, 10);
        location.increaseStock(secondProductId, 10);

        SellProductCommand command = command(
                new SellLineCommand(productId, 2),
                new SellLineCommand(secondProductId, 5)
        );

        givenProductExists(product);
        givenProductExists(secondProduct);
        givenShopLocations(location);
        givenAllocation(productId, 2, location, 2);
        givenAllocation(secondProductId, 5, location, 5);

        SellProductResult result = sellProductUseCase.sell(command);

        assertThat(location.getStockLevel(productId)).isEqualTo(8);
        assertThat(location.getStockLevel(secondProductId)).isEqualTo(5);

        Sale savedSale = captureSavedSale();
        assertThat(savedSale.getLines()).hasSize(2);
        assertSellProductResult(result, savedSale);

        List<StockMovement> movements = captureSavedMovements();
        assertThat(movements).hasSize(2);
        assertThat(movements).anySatisfy(movement ->
                assertExitMovement(movement, productId, location.getLocationId(), 2, savedSale));
        assertThat(movements).anySatisfy(movement ->
                assertExitMovement(movement, secondProductId, location.getLocationId(), 5, savedSale));

        List<DomainEvent> events = capturePublishedEvents();
        SaleCompleted saleCompleted = findEvent(events, SaleCompleted.class);
        assertThat(saleCompleted.lineItemCount()).isEqualTo(2);
    }

    @Test
    void should_split_single_product_sale_across_multiple_locations() {
        StorageLocation shopFloor = storageLocation(LocationType.SHOP_FLOOR, "Shop floor", 3);
        StorageLocation backstock = storageLocation(LocationType.BACKSTOCK, "Backstock", 3);

        shopFloor.increaseStock(productId, 3);
        backstock.increaseStock(productId, 7);

        SellProductCommand command = command(new SellLineCommand(productId, 4));

        givenProductExists(product);
        givenShopLocations(shopFloor, backstock);
        when(stockAllocationService.allocate(productId, 4, shopId))
                .thenReturn(List.of(
                        AllocationResult.of(shopFloor.getLocationId(), 3),
                        AllocationResult.of(backstock.getLocationId(), 1)
                ));

        sellProductUseCase.sell(command);

        assertThat(shopFloor.getStockLevel(productId)).isEqualTo(0);
        assertThat(backstock.getStockLevel(productId)).isEqualTo(6);

        Sale savedSale = captureSavedSale();
        List<StockMovement> movements = captureSavedMovements();

        assertThat(movements).hasSize(2);
        assertThat(movements).anySatisfy(movement ->
                assertExitMovement(movement, productId, shopFloor.getLocationId(), 3, savedSale));
        assertThat(movements).anySatisfy(movement ->
                assertExitMovement(movement, productId, backstock.getLocationId(), 1, savedSale));

        List<DomainEvent> events = capturePublishedEvents();
        ShopFloorLow shopFloorLow = findEvent(events, ShopFloorLow.class);
        assertThat(shopFloorLow.productId()).isEqualTo(productId);
        assertThat(shopFloorLow.locationId()).isEqualTo(shopFloor.getLocationId());
        assertThat(shopFloorLow.currentQuantity()).isEqualTo(0);
        assertThat(shopFloorLow.lowStockIndicator()).isEqualTo(3);
    }

    @Test
    void should_publish_low_stock_alert_when_global_stock_falls_below_threshold() {
        Product thresholdProduct = product(productId, "Oil Filter", "REF-123", 5, "15.00");

        StorageLocation shopFloor = storageLocation(LocationType.SHOP_FLOOR, "Shop floor", 3);
        StorageLocation backstock = storageLocation(LocationType.BACKSTOCK, "Backstock", 3);

        shopFloor.increaseStock(productId, 3);
        backstock.increaseStock(productId, 7);

        SellProductCommand command = command(new SellLineCommand(productId, 6));

        givenProductExists(thresholdProduct);
        givenShopLocations(shopFloor, backstock);
        when(stockAllocationService.allocate(productId, 6, shopId))
                .thenReturn(List.of(
                        AllocationResult.of(shopFloor.getLocationId(), 3),
                        AllocationResult.of(backstock.getLocationId(), 3)
                ));

        sellProductUseCase.sell(command);

        List<DomainEvent> events = capturePublishedEvents();

        LowStockAlert alert = findEvent(events, LowStockAlert.class);
        assertThat(alert.productId()).isEqualTo(productId);
        assertThat(alert.productName()).isEqualTo("Oil Filter");
        assertThat(alert.globalQuantity()).isEqualTo(4);
        assertThat(alert.threshold()).isEqualTo(5);
    }

    @Test
    void should_not_publish_low_stock_alert_when_global_stock_stays_at_threshold() {
        Product thresholdProduct = product(productId, "Oil Filter", "REF-123", 6, "15.00");

        StorageLocation location = storageLocation(LocationType.SHOP_FLOOR, "Shop floor", 3);
        location.increaseStock(productId, 10);

        SellProductCommand command = command(new SellLineCommand(productId, 4));

        givenProductExists(thresholdProduct);
        givenShopLocations(location);
        givenAllocation(productId, 4, location, 4);

        sellProductUseCase.sell(command);

        List<DomainEvent> events = capturePublishedEvents();

        assertThat(location.getStockLevel(productId)).isEqualTo(6);
        assertThat(events).noneMatch(LowStockAlert.class::isInstance);
    }

    @Test
    void should_reject_sale_when_product_does_not_exist_without_side_effects() {
        SellProductCommand command = command(new SellLineCommand(productId, 4));
        givenShopLocations(storageLocation(LocationType.SHOP_FLOOR, "Shop floor", 3));

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellProductUseCase.sell(command))
                .isInstanceOf(ProductNotFoundException.class);

        verify(saleRepository, never()).save(any());
        verify(storageLocationRepository, never()).saveAll(anyList());
        verify(stockMovementRepository, never()).saveAll(anyList());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_reject_sale_when_allocation_references_unknown_location_without_saving_stock_or_movements() {
        StorageLocation knownLocation = storageLocation(LocationType.SHOP_FLOOR, "Shop floor", 3);
        knownLocation.increaseStock(productId, 10);

        LocationId unknownLocationId = LocationId.generate();

        SellProductCommand command = command(new SellLineCommand(productId, 4));

        givenProductExists(product);
        givenShopLocations(knownLocation);
        when(stockAllocationService.allocate(productId, 4, shopId))
                .thenReturn(List.of(AllocationResult.of(unknownLocationId, 4)));

        assertThatThrownBy(() -> sellProductUseCase.sell(command))
                .isInstanceOf(StorageNotFoundException.class);

        verify(storageLocationRepository, never()).saveAll(anyList());
        verify(stockMovementRepository, never()).saveAll(anyList());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_not_sell_inactive_product() {
        product.deactivate();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> sellProductUseCase
                .sell(command(new SellLineCommand(productId, 4))))
                .isInstanceOf(InactiveProductException.class);
        verify(saleRepository, never()).save(any());

    }

    private SellProductCommand command(SellLineCommand... lines) {
        return new SellProductCommand(sellerId, shopId, List.of(lines));
    }

    private Product product(
            ProductId productId,
            String name,
            String reference,
            int minimumGlobalThreshold,
            String price
    ) {
        return new Product(
                productId,
                name,
                reference,
                CategoryId.generate(),
                minimumGlobalThreshold,
                Money.create(new BigDecimal(price), Currency.getInstance("EUR"))
        );
    }

    private StorageLocation storageLocation(LocationType type, String label, int lowStockIndicator) {
        return new StorageLocation(
                LocationId.generate(),
                shopId,
                type,
                label,
                lowStockIndicator
        );
    }

    private void givenProductExists(Product product) {
        when(productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
    }

    private void givenShopLocations(StorageLocation... locations) {
        when(storageLocationRepository.findByShopId(shopId)).thenReturn(List.of(locations));
    }

    private void givenAllocation(
            ProductId productId,
            int requestedQuantity,
            StorageLocation location,
            int allocatedQuantity
    ) {
        when(stockAllocationService.allocate(productId, requestedQuantity, shopId))
                .thenReturn(List.of(AllocationResult.of(location.getLocationId(), allocatedQuantity)));
    }

    private Sale captureSavedSale() {
        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(saleCaptor.capture());
        return saleCaptor.getValue();
    }

    private List<StockMovement> captureSavedMovements() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockMovement>> movementCaptor = ArgumentCaptor.forClass(List.class);

        verify(stockMovementRepository).saveAll(movementCaptor.capture());
        return movementCaptor.getValue();
    }

    private List<DomainEvent> capturePublishedEvents() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);

        verify(eventPublisher).publish(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private void assertExitMovement(
            StockMovement movement,
            ProductId expectedProductId,
            LocationId expectedSourceLocationId,
            int expectedQuantity,
            Sale savedSale
    ) {
        assertThat(movement.getMovementType()).isEqualTo(MovementType.EXIT);
        assertThat(movement.getProductId()).isEqualTo(expectedProductId);
        assertThat(movement.getQuantity()).isEqualTo(expectedQuantity);
        assertThat(movement.getPerformedBy()).isEqualTo(sellerId);
        assertThat(movement.getSourceLocationId()).isPresent().contains(expectedSourceLocationId);
        assertThat(movement.getDestinationLocationId()).isEmpty();
        assertThat(movement.getSaleId()).isPresent().contains(savedSale.getSaleId());
    }

    private void assertSellProductResult(SellProductResult result, Sale savedSale) {
        assertThat(result.saleId()).isEqualTo(savedSale.getSaleId());
        assertThat(result.sellerId()).isEqualTo(savedSale.getSoldBy());
        assertThat(result.lines()).containsExactlyElementsOf(savedSale.getLines());
        assertThat(result.totalAmount().getAmount()).isEqualByComparingTo(savedSale.getTotalAmount().getAmount());
        assertThat(result.totalAmount().getCurrency()).isEqualTo(savedSale.getTotalAmount().getCurrency());
        assertThat(result.createdAt()).isEqualTo(savedSale.getOccurredAt());
    }

    private void assertPersistenceHappensBeforePublication() {
        InOrder inOrder = inOrder(
                saleRepository,
                storageLocationRepository,
                stockMovementRepository,
                eventPublisher
        );

        inOrder.verify(saleRepository).save(any(Sale.class));
        inOrder.verify(storageLocationRepository).saveAll(anyList());
        inOrder.verify(stockMovementRepository).saveAll(anyList());
        inOrder.verify(eventPublisher).publish(anyList());
    }

    private <T extends DomainEvent> T findEvent(List<DomainEvent> events, Class<T> eventType) {
        return events.stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .findFirst()
                .orElseThrow();
    }
}
