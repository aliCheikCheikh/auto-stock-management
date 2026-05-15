package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.SellLineCommand;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
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
import java.util.*;
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
            EventPublisher eventPublisher
    ) {
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
    public SellProductResult sell(SellProductCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

        List<StorageLocation> shopLocations = storageLocationRepository.findByShopId(command.shopId());
        Map<LocationId, StorageLocation> locationsById = shopLocations.stream()
                .collect(Collectors.toMap(StorageLocation::getLocationId, location -> location));

        List<SaleLineInput> saleLineInputs = new ArrayList<>();
        List<PreparedLine> preparedLines = prepareLines(command, locationsById, saleLineInputs);

        Sale sale = Sale.create(command.sellerId(), saleLineInputs);

        List<StockMovement> generatedMovements = new ArrayList<>();
        Set<StorageLocation> changedLocations = new LinkedHashSet<>();
        List<DomainEvent> eventsToPublish = new ArrayList<>();
        Set<ProductId> alertedProducts = new LinkedHashSet<>();

        for (PreparedLine preparedLine : preparedLines) {
            Product product = preparedLine.product();

            for (AllocationResult allocation : preparedLine.allocations()) {
                StorageLocation location = locationsById.get(allocation.getLocationId());

                location.decreaseStock(product.getProductId(), allocation.getQuantity());
                changedLocations.add(location);

                generatedMovements.add(StockMovement.createExit(
                        product.getProductId(),
                        location.getLocationId(),
                        allocation.getQuantity(),
                        command.sellerId(),
                        sale.getSaleId()
                ));
            }

            int globalStock = calculateGlobalStock(shopLocations, product.getProductId());
            if (globalStock < product.getMinimumGlobalThreshold()
                    && alertedProducts.add(product.getProductId())) {
                eventsToPublish.add(new LowStockAlert(
                        product.getProductId(),
                        product.getName(),
                        globalStock,
                        product.getMinimumGlobalThreshold(),
                        LocalDateTime.now()
                ));
            }
        }

        saleRepository.save(sale);
        storageLocationRepository.saveAll(new ArrayList<>(changedLocations));
        stockMovementRepository.saveAll(generatedMovements);

        changedLocations.forEach(location -> eventsToPublish.addAll(location.pullEvents()));

        eventsToPublish.add(new SaleCompleted(
                sale.getSaleId(),
                sale.getTotalAmount(),
                sale.getLines().size(),
                sale.getSoldBy(),
                LocalDateTime.now()
        ));

        eventPublisher.publish(eventsToPublish);

        return new SellProductResult(
                sale.getSaleId(),
                sale.getSoldBy(),
                sale.getLines(),
                sale.getTotalAmount(),
                sale.getOccurredAt()
        );
    }

    private List<PreparedLine> prepareLines(
            SellProductCommand command,
            Map<LocationId, StorageLocation> locationsById,
            List<SaleLineInput> saleLineInputs
    ) {
        List<PreparedLine> preparedLines = new ArrayList<>();

        for (SellLineCommand line : command.lines()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ProductNotFoundException(line.productId()));

            List<AllocationResult> allocations = stockAllocationService.allocate(
                    line.productId(),
                    line.quantity(),
                    command.shopId()
            );

            validateAllocatedLocationsExist(allocations, locationsById);

            saleLineInputs.add(new SaleLineInput(
                    product.getProductId(),
                    line.quantity(),
                    product.getUnitPrice()
            ));

            preparedLines.add(new PreparedLine(product, allocations));
        }

        return preparedLines;
    }

    private void validateAllocatedLocationsExist(
            List<AllocationResult> allocations,
            Map<LocationId, StorageLocation> locationsById
    ) {
        for (AllocationResult allocation : allocations) {
            if (!locationsById.containsKey(allocation.getLocationId())) {
                throw new StorageNotFoundException(allocation.getLocationId());
            }
        }
    }

    private int calculateGlobalStock(List<StorageLocation> shopLocations, ProductId productId) {
        return shopLocations.stream()
                .mapToInt(location -> location.getStockLevel(productId))
                .sum();
    }

    private record PreparedLine(Product product, List<AllocationResult> allocations) {
    }
}
