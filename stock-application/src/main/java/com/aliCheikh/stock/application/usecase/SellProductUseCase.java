package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.SellLineCommand;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.domain.event.DomainEvent;
import com.aliCheikh.stock.domain.event.LowStockAlert;
import com.aliCheikh.stock.domain.event.SaleCompleted;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.exception.stock.StorageNotFoundException;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleLineInput;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.service.AllocationResult;
import com.aliCheikh.stock.domain.service.StockAllocationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;




/**
 * Use case for selling one or more products to a customer.
 *
 * <p>This use case orchestrates the complete sale workflow: it loads products,
 * delegates stock allocation to the domain service, creates the sale aggregate,
 * decreases stock from allocated locations, records EXIT movements, and publishes
 * sale-related domain events.</p>
 *
 * <p>Business rules enforced or coordinated by this use case:</p>
 * <ul>
 *     <li>each sold product must exist;</li>
 *     <li>stock allocation must provide enough stock for every requested line;</li>
 *     <li>allocated storage locations must belong to the target shop lookup result;</li>
 *     <li>one {@code EXIT} movement is recorded for each allocated location;</li>
 *     <li>a {@code SaleCompleted} event is published after the sale is persisted;</li>
 *     <li>{@code ShopFloorLow} events produced by storage locations are published;</li>
 *     <li>{@code LowStockAlert} is published when global stock falls below the product threshold.</li>
 * </ul>
 *
 * <p>Transaction management is owned by the infrastructure layer.</p>
 */
public class SellProductUseCase {

    private final StockAllocationService stockAllocationService;
    private final StorageLocationRepository storageLocationRepository;
    private final SaleRepository saleRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;

    public SellProductUseCase(
            StockAllocationService stockAllocationService,
            StorageLocationRepository storageLocationRepository,
            SaleRepository saleRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            EventPublisher eventPublisher) {
        this.stockAllocationService = Objects.requireNonNull(stockAllocationService, "stockAllocationService cannot be null");
        this.storageLocationRepository = Objects.requireNonNull(storageLocationRepository, "storageLocationRepository cannot be null");
        this.saleRepository = Objects.requireNonNull(saleRepository, "saleRepository cannot be null");
        this.stockMovementRepository = Objects.requireNonNull(stockMovementRepository, "stockMovementRepository cannot be null");
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
    }


    /**
     * Executes a multi-line sale.
     *
     * @param command validated sale request
     * @throws ProductNotFoundException if one sold product does not exist
     * @throws StorageNotFoundException if an allocation references a location not loaded for the shop
     */
    public void sell(SellProductCommand command) {
        List<DomainEvent> eventsToPublish = new ArrayList<>();
        List<SaleLineInput> lineInputs = new ArrayList<>();
        List<PreparedLine> preparedLines = new ArrayList<>();

        // 1. Load all shop locations into memory to avoid N+1 queries during allocation
        List<StorageLocation> shopLocations = storageLocationRepository.findByShopId(command.shopId());
        Map<LocationId, StorageLocation> locationsCache = shopLocations.stream()
                .collect(Collectors.toMap(StorageLocation::getLocationId, loc -> loc));

        // 2. PHASE 1: Preparation & Allocation (Fail-fast before any persistence)
        for (SellLineCommand line : command.lines()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ProductNotFoundException(line.productId()));

            List<AllocationResult> allocations = stockAllocationService.allocate(
                    line.productId(), line.quantity(), command.shopId()
            );

            preparedLines.add(new PreparedLine(product, allocations));

            lineInputs.add(new SaleLineInput(
                    product.getProductId(),
                    line.quantity(),
                    product.getUnitPrice()
            ));
        }

        // 3. Create and persist the Sale aggregate FIRST to obtain a valid SaleId
        Sale sale = Sale.create(command.sellerId(), lineInputs);
        saleRepository.save(sale);

        List<StockMovement> generatedMovements = new ArrayList<>();

        // 4. PHASE 2: Execution (Mutate state and generate traces)
        for (PreparedLine preparedLine : preparedLines) {

            // a. Apply state changes to locations in memory
            for (AllocationResult allocation : preparedLine.allocations()) {
                StorageLocation location = locationsCache.get(allocation.getLocationId());
                if (location == null) {
                    throw new StorageNotFoundException(allocation.getLocationId());
                }

                // Delegate state mutation to the Aggregate
                location.decreaseStock(preparedLine.product().getProductId(), allocation.getQuantity());

                // Pull operational events (e.g., ShopFloorLow)
                eventsToPublish.addAll(location.pullEvents());

                // Generate traceability movement linking to the created Sale
                StockMovement movement = StockMovement.createExit(
                        preparedLine.product().getProductId(),
                        location.getLocationId(),
                        allocation.getQuantity(),
                        command.sellerId(),
                        sale.getSaleId()
                );
                generatedMovements.add(movement);
            }

            // b. Evaluate Global Stock Strategy for alerts
            int globalStock = calculateGlobalStock(shopLocations, preparedLine.product().getProductId());
            if (globalStock < preparedLine.product().getMinimumGlobalThreshold()) {
                eventsToPublish.add(new LowStockAlert(
                        preparedLine.product().getProductId(),
                        preparedLine.product().getName(),
                        globalStock,
                        preparedLine.product().getMinimumGlobalThreshold(),
                        LocalDateTime.now()
                ));
            }
        }

        // 5. Batch persistence for high performance
        storageLocationRepository.saveAll(shopLocations);
        stockMovementRepository.saveAll(generatedMovements);

        // 6. Generate final workflow event
        eventsToPublish.add(new SaleCompleted(
                sale.getSaleId(),
                sale.getTotalAmount(),
                sale.getLines().size(),
                sale.getSoldBy(),
                LocalDateTime.now()
        ));

        // 7. Publish all accumulated domain events
        eventPublisher.publish(eventsToPublish);
    }

    /**
     * Calculates the true global stock for a product across the entire shop.
     * Uses the full list of shop locations to ensure accuracy, regardless of which locations were allocated.
     */
    private int calculateGlobalStock(List<StorageLocation> shopLocations, ProductId productId) {
        return shopLocations.stream()
                .mapToInt(loc -> loc.getStockLevel(productId))
                .sum();
    }

    /**
     * Internal structure to safely carry prepared data between Phase 1 (Validation) and Phase 2 (Execution).
     */
    private record PreparedLine(Product product, List<AllocationResult> allocations) {
    }
}