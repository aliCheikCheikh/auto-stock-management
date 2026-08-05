package com.aliCheikh.stock.infrastructure.config;

import com.aliCheikh.stock.application.port.CreditSaleDetailQueryPort;
import com.aliCheikh.stock.application.port.EventPublisher;
import com.aliCheikh.stock.application.port.ListSalesQueryPort;
import com.aliCheikh.stock.application.port.CustomerSearchQueryPort;
import com.aliCheikh.stock.application.port.DebtQueryPort;
import com.aliCheikh.stock.application.port.ProductSearchQueryPort;
import com.aliCheikh.stock.application.port.ProductStockQueryPort;
import com.aliCheikh.stock.application.port.StockLevelQueryPort;
import com.aliCheikh.stock.application.port.StockMovementQueryPort;
import com.aliCheikh.stock.application.port.StockReceiptImportCategoryQueryPort;
import com.aliCheikh.stock.application.port.StockReceiptImportExecutionLedger;
import com.aliCheikh.stock.application.port.StockReceiptImportFailureReporter;
import com.aliCheikh.stock.application.port.StockReceiptImportProductQueryPort;
import com.aliCheikh.stock.application.port.StockReceiptImportReader;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.service.StockReceiptImportExecutionFingerprint;
import com.aliCheikh.stock.application.service.StockReceiptImportRowPreparator;
import com.aliCheikh.stock.application.usecase.CreateCategoryUseCase;
import com.aliCheikh.stock.application.usecase.DeactivateProductUseCase;
import com.aliCheikh.stock.application.usecase.DeleteCategoryUseCase;
import com.aliCheikh.stock.application.usecase.ExecuteStockReceiptImportUseCase;
import com.aliCheikh.stock.application.usecase.RenameCategoryUseCase;
import com.aliCheikh.stock.application.usecase.GetProductStockLevelsUseCase;
import com.aliCheikh.stock.application.usecase.GetSessionContextUseCase;
import com.aliCheikh.stock.application.usecase.ListCategoriesUseCase;
import com.aliCheikh.stock.application.usecase.GetCustomerUseCase;
import com.aliCheikh.stock.application.usecase.GetCreditSaleDetailUseCase;
import com.aliCheikh.stock.application.usecase.ListDebtsUseCase;
import com.aliCheikh.stock.application.usecase.SearchCustomersUseCase;
import com.aliCheikh.stock.application.usecase.RecordPaymentUseCase;
import com.aliCheikh.stock.application.usecase.RegisterCustomerUseCase;
import com.aliCheikh.stock.application.usecase.ListSalesUseCase;
import com.aliCheikh.stock.application.usecase.ListStockLevelsUseCase;
import com.aliCheikh.stock.application.usecase.ListStockMovementsUseCase;
import com.aliCheikh.stock.application.usecase.PrepareStockReceiptImportUseCase;
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

import java.time.Clock;

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
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new ReceiveStockUseCase(
                productRepository,
                receivingService,
                stockMovementRepository,
                storageLocationRepository,
                eventPublisher,
                transactionRunner,
                clock
        );
    }

    @Bean
    public StockReceiptImportRowPreparator stockReceiptImportRowPreparator() {
        return new StockReceiptImportRowPreparator();
    }

    @Bean
    public PrepareStockReceiptImportUseCase prepareStockReceiptImportUseCase(
            StockReceiptImportReader reader,
            StockReceiptImportProductQueryPort productQueryPort,
            StockReceiptImportCategoryQueryPort categoryQueryPort,
            StorageLocationRepository storageLocationRepository,
            StockReceiptImportRowPreparator rowPreparator
    ) {
        return new PrepareStockReceiptImportUseCase(
                reader,
                productQueryPort,
                categoryQueryPort,
                storageLocationRepository,
                rowPreparator
        );
    }

    @Bean
    public StockReceiptImportExecutionFingerprint stockReceiptImportExecutionFingerprint() {
        return new StockReceiptImportExecutionFingerprint();
    }

    @Bean
    public ExecuteStockReceiptImportUseCase executeStockReceiptImportUseCase(
            PrepareStockReceiptImportUseCase prepareUseCase,
            ReceiveStockUseCase receiveStockUseCase,
            StockReceiptImportExecutionLedger ledger,
            StockReceiptImportFailureReporter failureReporter,
            TransactionRunner transactionRunner,
            StockReceiptImportExecutionFingerprint fingerprint
    ) {
        return new ExecuteStockReceiptImportUseCase(
                prepareUseCase,
                receiveStockUseCase,
                ledger,
                failureReporter,
                transactionRunner,
                fingerprint
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
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new SellProductUseCase(
                stockAllocationService,
                storageLocationRepository,
                saleRepository,
                stockMovementRepository,
                productRepository,
                customerRepository,
                eventPublisher,
                transactionRunner,
                clock
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
    public GetCustomerUseCase getCustomerUseCase(CustomerRepository customerRepository) {
        return new GetCustomerUseCase(customerRepository);
    }

    @Bean
    public SearchCustomersUseCase searchCustomersUseCase(CustomerSearchQueryPort customerSearchQueryPort) {
        return new SearchCustomersUseCase(customerSearchQueryPort);
    }

    @Bean
    public ListDebtsUseCase listDebtsUseCase(
            DebtQueryPort debtQueryPort,
            Clock clock
    ) {
        return new ListDebtsUseCase(debtQueryPort, clock);
    }

    @Bean
    public GetCreditSaleDetailUseCase getCreditSaleDetailUseCase(
            CreditSaleDetailQueryPort creditSaleDetailQueryPort
    ) {
        return new GetCreditSaleDetailUseCase(creditSaleDetailQueryPort);
    }

    @Bean
    public RecordPaymentUseCase recordPaymentUseCase(
            SaleRepository saleRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new RecordPaymentUseCase(saleRepository, transactionRunner, clock);
    }

    @Bean
    public CreateCategoryUseCase createCategoryUseCase(
            CategoryRepository categoryRepository,
            TransactionRunner transactionRunner
    ) {
        return new CreateCategoryUseCase(categoryRepository, transactionRunner);
    }

    @Bean
    public RenameCategoryUseCase renameCategoryUseCase(
            CategoryRepository categoryRepository,
            TransactionRunner transactionRunner
    ) {
        return new RenameCategoryUseCase(categoryRepository, transactionRunner);
    }

    @Bean
    public DeleteCategoryUseCase deleteCategoryUseCase(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            TransactionRunner transactionRunner
    ) {
        return new DeleteCategoryUseCase(categoryRepository, productRepository, transactionRunner);
    }

    @Bean
    public TransferStockUseCase transferStockUseCase(
            StorageLocationRepository storageLocationRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            EventPublisher eventPublisher,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new TransferStockUseCase(
                storageLocationRepository,
                stockMovementRepository,
                productRepository,
                eventPublisher,
                transactionRunner,
                clock
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
