package com.aliCheikh.stock.infrastructure.config;

import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.application.port.ListSalesQueryPort;
import com.aliCheikh.stock.application.port.StockLevelQueryPort;
import com.aliCheikh.stock.application.port.StockMovementQueryPort;
import com.aliCheikh.stock.application.usecase.*;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.domain.service.ReceivingService;
import com.aliCheikh.stock.domain.service.StockAllocationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public ReceivingService receivingService(StorageLocationRepository storageLocationRepository) {
        return new ReceivingService(storageLocationRepository);
    }

    @Bean
    public StockAllocationService stockAllocationService(StorageLocationRepository storageLocationRepository) {
        return new StockAllocationService(storageLocationRepository);
    }

    @Bean
    public ReceiveStockUseCase receiveStockUseCase(
            ProductRepository productRepository,
            ReceivingService receivingService,
            StockMovementRepository stockMovementRepository,
            StorageLocationRepository storageLocationRepository,
            EventPublisher eventPublisher
    ) {
        return new ReceiveStockUseCase(
                productRepository,
                receivingService,
                stockMovementRepository,
                storageLocationRepository,
                eventPublisher
        );
    }

    @Bean
    public SellProductUseCase sellProductUseCase(
            StockAllocationService stockAllocationService,
            StorageLocationRepository storageLocationRepository,
            SaleRepository saleRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            EventPublisher eventPublisher
    ) {
        return new SellProductUseCase(
                stockAllocationService,
                storageLocationRepository,
                saleRepository,
                stockMovementRepository,
                productRepository,
                eventPublisher
        );
    }

    @Bean
    public TransferStockUseCase transferStockUseCase(
            StorageLocationRepository storageLocationRepository,
            StockMovementRepository stockMovementRepository,
            EventPublisher eventPublisher
    ) {
        return new TransferStockUseCase(
                storageLocationRepository,
                stockMovementRepository,
                eventPublisher
        );
    }

    @Bean
    public ListStockMovementsUseCase listStockMovementsUseCase(
            StockMovementQueryPort stockMovementQueryPort
    ) {
        return new ListStockMovementsUseCase(stockMovementQueryPort);
    }

    @Bean
    public ListSalesUseCase listSalesUseCase(ListSalesQueryPort listSalesQueryPort) {
        return new ListSalesUseCase(listSalesQueryPort);
    }

    @Bean
    public ListStockLevelsUseCase listStockLevelsUseCase(StockLevelQueryPort stockLevelQueryPort) {
        return new ListStockLevelsUseCase(stockLevelQueryPort);
    }
}
