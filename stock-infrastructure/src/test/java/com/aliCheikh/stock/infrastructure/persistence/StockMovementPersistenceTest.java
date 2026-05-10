package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleLineInput;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.adapter.SaleJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.adapter.StockMovementJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ShopJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.SaleJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.mapper.StockMovementJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ShopJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.StockMovementJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.StorageLocationJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        SaleJpaRepositoryAdapter.class,
        SaleJpaMapper.class,
        StockMovementJpaRepositoryAdapter.class,
        StockMovementJpaMapper.class
})
class StockMovementPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StockMovementJpaRepositoryAdapter adapter;

    @Autowired
    private StockMovementJpaRepository movementRepository;

    @Autowired
    private SaleJpaRepositoryAdapter saleAdapter;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private ShopJpaRepository shopRepository;

    @Autowired
    private StorageLocationJpaRepository storageLocationRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CategoryId categoryId;
    private ProductId productId;
    private ShopId shopId;
    private LocationId sourceLocationId;
    private LocationId destinationLocationId;
    private UserId userId;
    private Money unitPrice;
    private Sale sale;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        productId = ProductId.generate();
        shopId = ShopId.generate();
        sourceLocationId = LocationId.generate();
        destinationLocationId = LocationId.generate();
        userId = UserId.generate();
        unitPrice = Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"));
        sale = Sale.create(
                userId,
                List.of(new SaleLineInput(productId, 1, unitPrice))
        );
    }

    @Test
    void should_save_entry_stock_movement() {
        saveReferenceData();

        StockMovement entry = StockMovement.createEntry(
                productId,
                destinationLocationId,
                10,
                userId
        );

        adapter.save(entry);
        flushAndClear();

        StockMovementJpaEntity persistedMovement = movementRepository.findById(entry.getMovementId().getValue())
                .orElseThrow();

        assertThat(persistedMovement.getProductId()).isEqualTo(productId.getValue());
        assertThat(persistedMovement.getMovementType()).isEqualTo(MovementType.ENTRY);
        assertThat(persistedMovement.getQuantity()).isEqualTo(10);
        assertThat(persistedMovement.getPerformedBy()).isEqualTo(userId.getValue());
        assertThat(persistedMovement.getSourceLocationId()).isNull();
        assertThat(persistedMovement.getDestinationLocationId()).isEqualTo(destinationLocationId.getValue());
        assertThat(persistedMovement.getSaleId()).isNull();
    }

    @Test
    void should_save_all_stock_movement_shapes() {
        saveReferenceData();
        saveSale();

        StockMovement entry = StockMovement.createEntry(
                productId,
                destinationLocationId,
                10,
                userId
        );
        StockMovement exit = StockMovement.createExit(
                productId,
                sourceLocationId,
                1,
                userId,
                sale.getSaleId()
        );
        StockMovement transfer = StockMovement.createTransfer(
                productId,
                sourceLocationId,
                destinationLocationId,
                5,
                userId
        );

        adapter.saveAll(List.of(entry, exit, transfer));
        flushAndClear();

        List<StockMovementJpaEntity> persistedMovements = movementRepository.findAll();

        assertThat(persistedMovements).hasSize(3);
        assertPersistedEntry(findByType(persistedMovements, MovementType.ENTRY));
        assertPersistedExit(findByType(persistedMovements, MovementType.EXIT));
        assertPersistedTransfer(findByType(persistedMovements, MovementType.TRANSFER));
    }

    private void saveReferenceData() {
        categoryRepository.save(CategoryJpaEntity.of(
                categoryId.getValue(),
                "Brakes"
        ));

        productRepository.save(ProductJpaEntity.of(
                productId.getValue(),
                "Brake pads",
                "BRK-PAD-001",
                categoryId.getValue(),
                5,
                unitPrice.getAmount(),
                unitPrice.getCurrency().getCurrencyCode()
        ));

        shopRepository.save(ShopJpaEntity.of(
                shopId.getValue(),
                "Nantes Shop",
                "42 Boulevard Gustave Roch"
        ));

        storageLocationRepository.save(StorageLocationJpaEntity.of(
                sourceLocationId.getValue(),
                shopId.getValue(),
                LocationType.BACKSTOCK,
                "Reserve A",
                3
        ));

        storageLocationRepository.save(StorageLocationJpaEntity.of(
                destinationLocationId.getValue(),
                shopId.getValue(),
                LocationType.SHOP_FLOOR,
                "Rayon A",
                3
        ));

        userRepository.save(UserJpaEntity.of(
                userId.getValue(),
                "seller",
                UserRole.SELLER
        ));

        flushAndClear();
    }

    private void saveSale() {
        saleAdapter.save(sale);
        flushAndClear();
    }

    private StockMovementJpaEntity findByType(
            List<StockMovementJpaEntity> movements,
            MovementType movementType
    ) {
        return movements.stream()
                .filter(movement -> movement.getMovementType() == movementType)
                .findFirst()
                .orElseThrow();
    }

    private void assertPersistedEntry(StockMovementJpaEntity movement) {
        assertThat(movement.getSourceLocationId()).isNull();
        assertThat(movement.getDestinationLocationId()).isEqualTo(destinationLocationId.getValue());
        assertThat(movement.getSaleId()).isNull();
        assertCommonMovementFields(movement, MovementType.ENTRY, 10);
    }

    private void assertPersistedExit(StockMovementJpaEntity movement) {
        assertThat(movement.getSourceLocationId()).isEqualTo(sourceLocationId.getValue());
        assertThat(movement.getDestinationLocationId()).isNull();
        assertThat(movement.getSaleId()).isEqualTo(sale.getSaleId().getValue());
        assertCommonMovementFields(movement, MovementType.EXIT, 1);
    }

    private void assertPersistedTransfer(StockMovementJpaEntity movement) {
        assertThat(movement.getSourceLocationId()).isEqualTo(sourceLocationId.getValue());
        assertThat(movement.getDestinationLocationId()).isEqualTo(destinationLocationId.getValue());
        assertThat(movement.getSaleId()).isNull();
        assertCommonMovementFields(movement, MovementType.TRANSFER, 5);
    }

    private void assertCommonMovementFields(
            StockMovementJpaEntity movement,
            MovementType expectedMovementType,
            int expectedQuantity
    ) {
        assertThat(movement.getProductId()).isEqualTo(productId.getValue());
        assertThat(movement.getMovementType()).isEqualTo(expectedMovementType);
        assertThat(movement.getQuantity()).isEqualTo(expectedQuantity);
        assertThat(movement.getPerformedBy()).isEqualTo(userId.getValue());
        assertThat(movement.getOccurredAt()).isNotNull();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
