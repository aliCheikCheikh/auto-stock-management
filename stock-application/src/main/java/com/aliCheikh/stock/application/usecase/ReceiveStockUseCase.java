package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.event.StockReceived;
import com.aliCheikh.stock.domain.event.StockReplenished;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.service.ReceivingEntry;
import com.aliCheikh.stock.domain.service.ReceivingService;
import com.aliCheikh.stock.application.dto.ProductInfo;
import com.aliCheikh.stock.application.dto.TargetLocation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Use Case for receiving stock from an external source (e.g., supplier delivery).
 * Handles both existing products and the creation of new products on-the-fly.
 */
public class ReceiveStockUseCase {

    private final ProductRepository productRepository;
    private final ReceivingService receivingService;
    private final StockMovementRepository stockMovementRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final EventPublisher eventPublisher;

    public ReceiveStockUseCase(
            ProductRepository productRepository,
            ReceivingService receivingService,
            StockMovementRepository stockMovementRepository,
            StorageLocationRepository storageLocationRepository,
            EventPublisher eventPublisher) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
        this.receivingService = Objects.requireNonNull(receivingService, "receivingService cannot be null");
        this.stockMovementRepository = Objects.requireNonNull(stockMovementRepository, "stockMovementRepository cannot be null");
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository, "storageLocationRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
    }

    public void execute(ReceiveStockCommand command) {
        // 1. Resolve or Create the Product
        Product product = resolveProduct(command);

        // 2. Translate Front-end intentions (TargetLocation) into strict Domain objects (ReceivingEntry)
        List<ReceivingEntry> strictEntries = command.distributions().stream()
                .map(target -> ReceivingEntry.of(product.getProductId(), target.locationId(), target.quantity()))
                .toList();

        // 3. Delegate state changes and movement generation to the Domain Service
        List<StockMovement> generatedMovements = receivingService.receive(strictEntries, command.userId());

        // 4. Persist the generated movements
        stockMovementRepository.saveAll(generatedMovements);

        // 5. Calculate total and publish Domain Events
        publishEvents(command, product);
    }

    /**
     * Resolves the product by reference, or creates a new one if info is provided.
     */
    private Product resolveProduct(ReceiveStockCommand command) {
        Optional<Product> existingProduct = productRepository.findByReference(command.productReference());

        if (existingProduct.isPresent()) {
            return existingProduct.get();
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

    /**
     * Prepares and publishes the appropriate Domain Events
     */
    private void publishEvents(ReceiveStockCommand command, Product product) {
        int totalReceived = command.distributions().stream()
                .mapToInt(TargetLocation::quantity)
                .sum();

        // Spec 3.5: Build the location breakdown map
        Map<LocationId, Integer> locationBreakdown = command.distributions().stream()
                .collect(Collectors.toMap(TargetLocation::locationId, TargetLocation::quantity));

        List<DomainEvent> eventsToPublish = new ArrayList<>();

        // a) Unconditional event: StockReceived (Now fully compliant with spec 3.5)
        eventsToPublish.add(new StockReceived(
                product.getProductId(),
                totalReceived,
                locationBreakdown,
                command.userId(),
                LocalDateTime.now()
        ));

        // b) Conditional event: StockReplenished (Now fully compliant with spec 3.2)
        int globalStock = calculateGlobalStock(product.getProductId());

        if (globalStock > product.getMinimumGlobalThreshold()) {
            eventsToPublish.add(new StockReplenished(
                    product.getProductId(),
                    product.getName(), // Added productName
                    globalStock,       // Now uses actual global quantity, not just received quantity
                    product.getMinimumGlobalThreshold(), // Added threshold
                    LocalDateTime.now()
            ));
        }

        eventPublisher.publish(eventsToPublish);
    }

    /**
     * Calculates the true global stock by summing up the stock across all locations.
     */
    private int calculateGlobalStock(ProductId productId) {
        // Depending on your repository port interface, you might use findAll()
        // or a specific method like findByProductId(productId).
        return storageLocationRepository.findAll().stream()
                .mapToInt(loc -> loc.getStockLevel(productId))
                .sum();
    }
}