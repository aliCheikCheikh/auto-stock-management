package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ProductInfo;
import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.ReceiveStockResult;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.event.StockReceived;
import com.aliCheikh.stock.domain.event.StockReplenished;
import com.aliCheikh.stock.domain.exception.product.DuplicateProductNameException;
import com.aliCheikh.stock.domain.exception.product.DuplicateProductReferenceException;
import com.aliCheikh.stock.domain.exception.product.InactiveProductException;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.service.ReceivingEntry;
import com.aliCheikh.stock.domain.service.ReceivingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReceiveStockUseCaseTest {

    private static final Instant BUSINESS_INSTANT = Instant.parse("2026-08-05T10:15:30Z");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Ndjamena");
    private static final Clock BUSINESS_CLOCK = Clock.fixed(BUSINESS_INSTANT, BUSINESS_ZONE);
    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.ofInstant(BUSINESS_INSTANT, BUSINESS_ZONE);

    private ProductRepository productRepository;
    private ReceivingService receivingService;
    private StockMovementRepository stockMovementRepository;
    private StorageLocationRepository storageLocationRepository;
    private EventPublisher eventPublisher;
    private TransactionRunner transactionRunner;

    private ReceiveStockUseCase receiveStockUseCase;

    private ProductId productId;
    private ShopId shopId;
    private UserId userId;
    private LocationId shopFloorId;
    private LocationId backStockId;
    private String productReference;
    private Product product;
    private ReceiveStockCommand command;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        receivingService = mock(ReceivingService.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        storageLocationRepository = mock(StorageLocationRepository.class);
        eventPublisher = mock(EventPublisher.class);
        transactionRunner = new TransactionRunner() {
            @Override
            public <T> T execute(Supplier<T> work) {
                return work.get();   // exécute le travail, sans vraie transaction
            }
        };

        receiveStockUseCase = new ReceiveStockUseCase(
                productRepository,
                receivingService,
                stockMovementRepository,
                storageLocationRepository,
                eventPublisher,
                transactionRunner,
                BUSINESS_CLOCK
        );

        productId = ProductId.generate();
        shopId = ShopId.generate();
        userId = UserId.generate();
        shopFloorId = LocationId.generate();
        backStockId = LocationId.generate();
        productReference = "REF-123";

        product = product(productId, "Oil Filter", productReference, 40, "10.00");

        command = new ReceiveStockCommand(
                productReference,
                null,
                shopId,
                userId,
                List.of(
                        new TargetLocation(shopFloorId, 15),
                        new TargetLocation(backStockId, 35)
                )
        );
    }

    @Test
    void should_receive_stock_for_existing_product_and_publish_stock_received() {
        MovementId firstMovementId = MovementId.generate();
        MovementId secondMovementId = MovementId.generate();
        List<StockMovement> generatedMovements = generatedMovements(firstMovementId, secondMovementId);

        when(productRepository.findByReference(productReference)).thenReturn(Optional.of(product));
        when(receivingService.receive(anyList(), eq(userId), eq(BUSINESS_TIME))).thenReturn(generatedMovements);
        givenGlobalStock(productId, 15, 35);

        ReceiveStockResult result = receiveStockUseCase.execute(command);

        verify(productRepository, never()).save(any(Product.class));
        verify(stockMovementRepository).saveAll(generatedMovements);

        List<ReceivingEntry> receivingEntries = captureReceivingEntries();
        assertThat(receivingEntries).hasSize(2);
        assertThat(receivingEntries).containsExactly(
                ReceivingEntry.of(productId, shopFloorId, 15),
                ReceivingEntry.of(productId, backStockId, 35)
        );

        List<DomainEvent> events = capturePublishedEvents();
        StockReceived stockReceived = findEvent(events, StockReceived.class);

        assertThat(stockReceived.productId()).isEqualTo(productId);
        assertThat(stockReceived.totalQuantityReceived()).isEqualTo(50);
        assertThat(stockReceived.receivedBy()).isEqualTo(userId);
        assertThat(stockReceived.occurredAt()).isEqualTo(BUSINESS_TIME);
        assertThat(stockReceived.locationBreakdown())
                .containsEntry(shopFloorId, 15)
                .containsEntry(backStockId, 35);
        assertReceiveStockResult(result, productId, firstMovementId, secondMovementId);
    }

    @Test
    void should_create_product_when_reference_is_unknown_and_product_info_is_provided() {
        ProductInfo newProductInfo = new ProductInfo(
                "Oil Filter",
                productReference,
                CategoryId.generate(),
                Money.create(new BigDecimal("10.00"), Currency.getInstance("EUR")),
                40
        );

        ReceiveStockCommand newProductCommand = new ReceiveStockCommand(
                productReference,
                newProductInfo,
                shopId,
                userId,
                List.of(
                        new TargetLocation(shopFloorId, 15),
                        new TargetLocation(backStockId, 35)
                )
        );

        MovementId firstMovementId = MovementId.generate();
        MovementId secondMovementId = MovementId.generate();
        List<StockMovement> generatedMovements = generatedMovements(firstMovementId, secondMovementId);

        when(productRepository.findByReference(productReference)).thenReturn(Optional.empty());
        when(receivingService.receive(anyList(), eq(userId), eq(BUSINESS_TIME))).thenReturn(generatedMovements);

        ReceiveStockResult result = receiveStockUseCase.execute(newProductCommand);

        Product savedProduct = captureSavedProduct();
        assertThat(savedProduct.getName()).isEqualTo("Oil Filter");
        assertThat(savedProduct.getReference()).isEqualTo(productReference);
        assertThat(savedProduct.getMinimumGlobalThreshold()).isEqualTo(40);

        verify(stockMovementRepository).saveAll(generatedMovements);

        List<ReceivingEntry> receivingEntries = captureReceivingEntries();
        assertThat(receivingEntries).containsExactly(
                ReceivingEntry.of(savedProduct.getProductId(), shopFloorId, 15),
                ReceivingEntry.of(savedProduct.getProductId(), backStockId, 35)
        );

        List<DomainEvent> events = capturePublishedEvents();
        StockReceived stockReceived = findEvent(events, StockReceived.class);
        assertThat(stockReceived.productId()).isEqualTo(savedProduct.getProductId());
        assertReceiveStockResult(result, savedProduct.getProductId(), firstMovementId, secondMovementId);
    }

    @Test
    void should_reject_reception_when_product_is_unknown_and_no_product_info_is_provided() {
        when(productRepository.findByReference(productReference)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiveStockUseCase.execute(command))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(receivingService);
        verify(stockMovementRepository, never()).saveAll(anyList());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_publish_stock_replenished_when_global_stock_is_above_threshold() {
        List<StockMovement> generatedMovements = generatedMovements();

        when(productRepository.findByReference(productReference)).thenReturn(Optional.of(product));
        when(receivingService.receive(anyList(), eq(userId), eq(BUSINESS_TIME))).thenReturn(generatedMovements);
        givenGlobalStock(productId, 15, 35);

        receiveStockUseCase.execute(command);

        List<DomainEvent> events = capturePublishedEvents();

        StockReplenished replenished = findEvent(events, StockReplenished.class);
        assertThat(replenished.productId()).isEqualTo(productId);
        assertThat(replenished.productName()).isEqualTo("Oil Filter");
        assertThat(replenished.globalQuantity()).isEqualTo(50);
        assertThat(replenished.threshold()).isEqualTo(40);
    }

    @Test
    void should_not_publish_stock_replenished_when_global_stock_is_at_threshold() {
        Product thresholdProduct = product(productId, "Oil Filter", productReference, 50, "10.00");
        List<StockMovement> generatedMovements = generatedMovements();

        when(productRepository.findByReference(productReference)).thenReturn(Optional.of(thresholdProduct));
        when(receivingService.receive(anyList(), eq(userId), eq(BUSINESS_TIME))).thenReturn(generatedMovements);
        givenGlobalStock(productId, 15, 35);

        receiveStockUseCase.execute(command);

        List<DomainEvent> events = capturePublishedEvents();

        assertThat(events).anyMatch(StockReceived.class::isInstance);
        assertThat(events).noneMatch(StockReplenished.class::isInstance);
    }

    @Test
    void should_save_generated_movements_before_publishing_events() {
        List<StockMovement> generatedMovements = generatedMovements();

        when(productRepository.findByReference(productReference)).thenReturn(Optional.of(product));
        when(receivingService.receive(anyList(), eq(userId), eq(BUSINESS_TIME))).thenReturn(generatedMovements);
        givenGlobalStock(productId, 15, 35);

        receiveStockUseCase.execute(command);

        InOrder inOrder = inOrder(stockMovementRepository, eventPublisher);
        inOrder.verify(stockMovementRepository).saveAll(generatedMovements);
        inOrder.verify(eventPublisher).publish(anyList());
    }

    @Test
    void should_reject_reception_when_existing_product_is_inactive() {
        product.deactivate();
        when(productRepository.findByReference(productReference)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> receiveStockUseCase.execute(command))
                .isInstanceOf(InactiveProductException.class);

        verify(stockMovementRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_new_product_when_reference_already_exists() {
        ProductInfo info = new ProductInfo(
                "Filtre à huile", "REF-DUP", CategoryId.generate(),
                Money.create(new BigDecimal("2500.00"), Currency.getInstance("XAF")), 5);
        ReceiveStockCommand command = new ReceiveStockCommand(
                "REF-DUP", info, ShopId.generate(), UserId.generate(),
                List.of(new TargetLocation(LocationId.generate(), 10)));

        when(productRepository.findByReference("REF-DUP")).thenReturn(Optional.of(mock(Product.class)));

        assertThatThrownBy(() -> receiveStockUseCase.execute(command))
                .isInstanceOf(DuplicateProductReferenceException.class);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void should_reject_new_product_when_name_already_exists() {
        ProductInfo info = new ProductInfo(
                "Filtre à huile", "REF-NEW", CategoryId.generate(),
                Money.create(new BigDecimal("2500.00"), Currency.getInstance("XAF")), 5);
        ReceiveStockCommand command = new ReceiveStockCommand(
                "REF-NEW", info, ShopId.generate(), UserId.generate(),
                List.of(new TargetLocation(LocationId.generate(), 10)));

        when(productRepository.findByReference("REF-NEW")).thenReturn(Optional.empty());
        when(productRepository.existsByName("Filtre à huile")).thenReturn(true);

        assertThatThrownBy(() -> receiveStockUseCase.execute(command))
                .isInstanceOf(DuplicateProductNameException.class);

        verify(productRepository, never()).save(any(Product.class));
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

    private List<StockMovement> generatedMovements() {
        return List.of(mock(StockMovement.class), mock(StockMovement.class));
    }

    private List<StockMovement> generatedMovements(MovementId firstMovementId, MovementId secondMovementId) {
        List<StockMovement> generatedMovements = generatedMovements();
        when(generatedMovements.get(0).getMovementId()).thenReturn(firstMovementId);
        when(generatedMovements.get(1).getMovementId()).thenReturn(secondMovementId);
        return generatedMovements;
    }

    private void assertReceiveStockResult(
            ReceiveStockResult result,
            ProductId expectedProductId,
            MovementId... expectedMovementIds
    ) {
        assertThat(result.productId()).isEqualTo(expectedProductId);
        assertThat(result.totalReceived()).isEqualTo(50);
        assertThat(result.movementIds()).containsExactly(expectedMovementIds);
        assertThat(result.acceptedAt()).isEqualTo(BUSINESS_INSTANT);
    }

    private void givenGlobalStock(ProductId productId, int firstLocationQuantity, int secondLocationQuantity) {
        StorageLocation firstLocation = mock(StorageLocation.class);
        when(firstLocation.getStockLevel(productId)).thenReturn(firstLocationQuantity);

        StorageLocation secondLocation = mock(StorageLocation.class);
        when(secondLocation.getStockLevel(productId)).thenReturn(secondLocationQuantity);

        when(storageLocationRepository.findByShopId(shopId))
                .thenReturn(List.of(firstLocation, secondLocation));
    }

    private List<ReceivingEntry> captureReceivingEntries() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReceivingEntry>> entriesCaptor = ArgumentCaptor.forClass(List.class);

        verify(receivingService).receive(entriesCaptor.capture(), eq(userId), eq(BUSINESS_TIME));
        return entriesCaptor.getValue();
    }

    private Product captureSavedProduct() {
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(productCaptor.capture());
        return productCaptor.getValue();
    }

    private List<DomainEvent> capturePublishedEvents() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DomainEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);

        verify(eventPublisher).publish(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private <T extends DomainEvent> T findEvent(List<DomainEvent> events, Class<T> eventType) {
        return events.stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .findFirst()
                .orElseThrow();
    }
}
