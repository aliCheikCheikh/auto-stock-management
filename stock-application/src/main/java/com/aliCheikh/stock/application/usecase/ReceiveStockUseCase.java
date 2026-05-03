package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ProductInfo;
import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.TargetLocation;
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
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.service.ReceivingEntry;
import com.aliCheikh.stock.domain.service.ReceivingService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Use case for receiving supplier stock into shop storage locations.
 *
 * <p>This use case supports two receiving flows: receiving stock for an existing
 * product found by reference, or creating a new catalog product before receiving
 * its first stock quantities.</p>
 *
 * <p>Business rules enforced or coordinated by this use case:</p>
 * <ul>
 *     <li>an existing product is resolved by business reference;</li>
 *     <li>a new product is created only when product information is provided;</li>
 *     <li>received quantities are distributed across requested target locations;</li>
 *     <li>one {@code ENTRY} movement is recorded for each target location;</li>
 *     <li>{@code StockReceived} is always published after a successful reception;</li>
 *     <li>{@code StockReplenished} is published when global stock rises above the product threshold.</li>
 * </ul>
 *
 * <p>Transaction management is owned by the infrastructure layer.</p>
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


    /**
     * Executes a stock reception for an existing or newly created product.
     *
     * @param command validated stock reception request
     * @throws ProductNotFoundException if the product reference is unknown and no new product information is provided
     */
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
        int globalStock = calculateGlobalStock(product.getProductId(), command.shopId());

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
    private int calculateGlobalStock(ProductId productId, ShopId shopId) {
        // Depending on your repository port interface, you might use findAll()
        // or a specific method like findByProductId(productId).
        return storageLocationRepository.findByShopId(shopId).stream()
                .mapToInt(loc -> loc.getStockLevel(productId))
                .sum();
    }
}