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
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.service.ReceivingEntry;
import com.aliCheikh.stock.domain.service.ReceivingService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReceiveStockUseCase {

    private final ProductRepository productRepository;
    private final ReceivingService receivingService;
    private final StockMovementRepository stockMovementRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final EventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public ReceiveStockUseCase(
            ProductRepository productRepository,
            ReceivingService receivingService,
            StockMovementRepository stockMovementRepository,
            StorageLocationRepository storageLocationRepository,
            EventPublisher eventPublisher,
            TransactionRunner transactionRunner
    ) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
        this.receivingService = Objects.requireNonNull(receivingService, "receivingService cannot be null");
        this.stockMovementRepository = Objects.requireNonNull(stockMovementRepository, "stockMovementRepository cannot be null");
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository, "storageLocationRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
    }

    public ReceiveStockResult execute(ReceiveStockCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        return transactionRunner.execute(() -> {
            Product product = resolveProduct(command);
            List<ReceivingEntry> entries = toReceivingEntries(command, product);
            List<StockMovement> movements = receivingService.receive(entries, command.userId());

            stockMovementRepository.saveAll(movements);

            publishEvents(command, product);

            int totalReceived = command.distributions().stream()
                    .mapToInt(TargetLocation::quantity)
                    .sum();
            List<MovementId> movementIds = movements.stream()
                    .map(StockMovement::getMovementId)
                    .toList();
            Instant acceptedAt = Instant.now();

            return new ReceiveStockResult(
                    product.getProductId(),
                    totalReceived,
                    movementIds,
                    acceptedAt
            );
        });
    }

    private Product resolveProduct(ReceiveStockCommand command) {
        Optional<Product> existingProduct = productRepository.findByReference(command.productReference());

        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();
            product.ensureActive();
            return product;
        }

        if (command.newProductInfo() == null) {
            throw new ProductNotFoundException(command.productReference());
        }

        ProductInfo info = command.newProductInfo();
        Product newProduct = new Product(
                ProductId.generate(),
                info.name(),
                info.reference(),
                info.categoryId(),
                info.minimumGlobalThreshold(),
                info.unitPrice()
        );

        productRepository.save(newProduct);
        return newProduct;
    }

    private List<ReceivingEntry> toReceivingEntries(ReceiveStockCommand command, Product product) {
        return command.distributions().stream()
                .map(target -> ReceivingEntry.of(
                        product.getProductId(),
                        target.locationId(),
                        target.quantity()
                ))
                .toList();
    }

    private void publishEvents(ReceiveStockCommand command, Product product) {
        ReceptionSummary summary = summarizeReception(command);
        List<DomainEvent> eventsToPublish = new ArrayList<>();

        eventsToPublish.add(new StockReceived(
                product.getProductId(),
                summary.totalReceived(),
                summary.locationBreakdown(),
                command.userId(),
                LocalDateTime.now()
        ));

        int globalStock = calculateGlobalStock(product.getProductId(), command.shopId());

        if (globalStock > product.getMinimumGlobalThreshold()) {
            eventsToPublish.add(new StockReplenished(
                    product.getProductId(),
                    product.getName(),
                    globalStock,
                    product.getMinimumGlobalThreshold(),
                    LocalDateTime.now()
            ));
        }

        eventPublisher.publish(eventsToPublish);
    }

    private ReceptionSummary summarizeReception(ReceiveStockCommand command) {
        int totalReceived = command.distributions().stream()
                .mapToInt(TargetLocation::quantity)
                .sum();

        Map<LocationId, Integer> locationBreakdown = command.distributions().stream()
                .collect(Collectors.toMap(
                        TargetLocation::locationId,
                        TargetLocation::quantity
                ));

        return new ReceptionSummary(totalReceived, locationBreakdown);
    }

    private int calculateGlobalStock(ProductId productId, ShopId shopId) {
        return storageLocationRepository.findByShopId(shopId).stream()
                .mapToInt(location -> location.getStockLevel(productId))
                .sum();
    }

    private record ReceptionSummary(
            int totalReceived,
            Map<LocationId, Integer> locationBreakdown
    ) {
    }
}
