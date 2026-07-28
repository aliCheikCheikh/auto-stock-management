package com.aliCheikh.stock.infrastructure.config;

import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.application.port.ListSalesQueryPort;
import com.aliCheikh.stock.application.port.OutstandingDebtQueryPort;
import com.aliCheikh.stock.application.port.ProductSearchQueryPort;
import com.aliCheikh.stock.application.port.ProductStockQueryPort;
import com.aliCheikh.stock.application.port.StockLevelQueryPort;
import com.aliCheikh.stock.application.port.StockMovementQueryPort;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.usecase.DeactivateProductUseCase;
import com.aliCheikh.stock.application.usecase.GetProductStockLevelsUseCase;
import com.aliCheikh.stock.application.usecase.GetSessionContextUseCase;
import com.aliCheikh.stock.application.usecase.ListCategoriesUseCase;
import com.aliCheikh.stock.application.usecase.ListOutstandingDebtsUseCase;
import com.aliCheikh.stock.application.usecase.RegisterCustomerUseCase;
import com.aliCheikh.stock.application.usecase.ListSalesUseCase;
import com.aliCheikh.stock.application.usecase.ListStockLevelsUseCase;
import com.aliCheikh.stock.application.usecase.ListStockMovementsUseCase;
import com.aliCheikh.stock.application.usecase.ReceiveStockUseCase;
import com.aliCheikh.stock.application.usecase.SearchProductsUseCase;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.application.usecase.TransferStockUseCase;
import com.aliCheikh.stock.application.usecase.UpdateProductUseCase;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;
import com.aliCheikh.stock.domain.model.customer.port.CustomerRepository;
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
    public GetSessionContextUseCase getSessionContextUseCase(StorageLocationRepository storageLocationRepository) {
        return new GetSessionContextUseCase(storageLocationRepository);
    }

    @Bean
    public ReceiveStockUseCase receiveStockUseCase(
            ProductRepository productRepository,
            ReceivingService receivingService,
            StockMovementRepository stockMovementRepository,
            StorageLocationRepository storageLocationRepository,
            EventPublisher eventPublisher,
            TransactionRunner transactionRunner
    ) {
        return new ReceiveStockUseCase(
                productRepository,
                receivingService,
                stockMovementRepository,
                storageLocationRepository,
                eventPublisher,
                transactionRunner
        );
    }

    @Bean
    public SellProductUseCase sellProductUseCase(
            StockAllocationService stockAllocationService,
            StorageLocationRepository storageLocationRepository,
            SaleRepository saleRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            EventPublisher eventPublisher,
            TransactionRunner transactionRunner
    ) {
        return new SellProductUseCase(
                stockAllocationService,
                storageLocationRepository,
                saleRepository,
                stockMovementRepository,
                productRepository,
                customerRepository,
                eventPublisher,
                transactionRunner
        );
    }

    @Bean
    public RegisterCustomerUseCase registerCustomerUseCase(
            CustomerRepository customerRepository,
            TransactionRunner transactionRunner
    ) {
        return new RegisterCustomerUseCase(customerRepository, transactionRunner);
    }

    @Bean
    public ListOutstandingDebtsUseCase listOutstandingDebtsUseCase(
            OutstandingDebtQueryPort outstandingDebtQueryPort
    ) {
        return new ListOutstandingDebtsUseCase(outstandingDebtQueryPort);
    }

    @Bean
    public TransferStockUseCase transferStockUseCase(
            StorageLocationRepository storageLocationRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            EventPublisher eventPublisher,
            TransactionRunner transactionRunner
    ) {
        return new TransferStockUseCase(
                storageLocationRepository,
                stockMovementRepository,
                productRepository,
                eventPublisher,
                transactionRunner
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

    @Bean
    public GetProductStockLevelsUseCase getProductStockLevelsUseCase(ProductStockQueryPort productStockQueryPort) {
        return new GetProductStockLevelsUseCase(productStockQueryPort);
    }

    @Bean
    public DeactivateProductUseCase deactivateProductUseCase(ProductRepository productRepository) {
        return new DeactivateProductUseCase(productRepository);
    }

    @Bean
    public UpdateProductUseCase updateProductUseCase(ProductRepository productRepository) {
        return new UpdateProductUseCase(productRepository);
    }

    @Bean
    ListCategoriesUseCase listCategoriesUseCase(CategoryRepository categoryRepository) {
        return new ListCategoriesUseCase(categoryRepository);
    }

    @Bean
    public SearchProductsUseCase searchProductsUseCase(ProductSearchQueryPort productSearchQueryPort) {
        return new SearchProductsUseCase(productSearchQueryPort);
    }
}
