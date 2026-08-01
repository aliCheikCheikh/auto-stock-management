package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.OperationId;
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
import com.aliCheikh.stock.infrastructure.persistence.adapter.StockMovementQueryJpaAdapter;
import com.aliCheikh.stock.infrastructure.persistence.adapter.StockMovementJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.adapter.SaleSettlementResolver;
import com.aliCheikh.stock.infrastructure.persistence.adapter.UserDisplayNameResolver;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.CustomerJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ShopJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.SaleJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.mapper.StockMovementJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.CustomerJpaRepository;
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
import java.time.LocalDateTime;
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
        StockMovementQueryJpaAdapter.class,
        StockMovementJpaMapper.class,
        // L'adapter de lecture nomme désormais l'auteur de chaque mouvement et annonce
        // l'état de règlement des ventes : sans ces collaborateurs, le contexte de la
        // tranche ne démarre pas.
        UserDisplayNameResolver.class,
        SaleSettlementResolver.class
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
    private StockMovementQueryJpaAdapter queryAdapter;

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
    private CustomerJpaRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CategoryId categoryId;
    private ProductId productId;
    private ShopId shopId;
    private LocationId sourceLocationId;
    private LocationId destinationLocationId;
    private UserId userId;
    private CustomerId customerId;
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
        customerId = CustomerId.generate();
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
                userId,
                OperationId.generate());

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
                userId,
                OperationId.generate());
        StockMovement exit = StockMovement.createExit(
                productId,
                sourceLocationId,
                1,
                userId,
                sale.getSaleId(),
                OperationId.generate());
        StockMovement transfer = StockMovement.createTransfer(
                productId,
                sourceLocationId,
                destinationLocationId,
                5,
                userId,
                OperationId.generate());

        adapter.saveAll(List.of(entry, exit, transfer));
        flushAndClear();

        List<StockMovementJpaEntity> persistedMovements = movementRepository.findAll();

        assertThat(persistedMovements).hasSize(3);
        assertPersistedEntry(findByType(persistedMovements, MovementType.ENTRY));
        assertPersistedExit(findByType(persistedMovements, MovementType.EXIT));
        assertPersistedTransfer(findByType(persistedMovements, MovementType.TRANSFER));
    }

    @Test
    void should_find_stock_movements_by_query() {
        saveReferenceData();

        StockMovement entry = StockMovement.rehydrate(
                MovementId.generate(),
                productId,
                null,
                destinationLocationId,
                MovementType.ENTRY,
                10,
                userId,
                LocalDateTime.of(2026, 5, 1, 10, 0),
                null,
                OperationId.generate());
        StockMovement transfer = StockMovement.rehydrate(
                MovementId.generate(),
                productId,
                sourceLocationId,
                destinationLocationId,
                MovementType.TRANSFER,
                5,
                userId,
                LocalDateTime.of(2026, 5, 2, 10, 0),
                null,
                OperationId.generate());

        adapter.saveAll(List.of(entry, transfer));
        flushAndClear();

        ListStockMovementsQuery query = new ListStockMovementsQuery(
                0,
                10,
                List.of("executedAt,asc"),
                productId,
                sourceLocationId,
                MovementType.TRANSFER,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 3, 0, 0)
        );

        PageResult<StockMovementView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        StockMovementView movement = result.content().get(0);
        assertThat(movement.movementId()).isEqualTo(transfer.getMovementId());
        assertThat(movement.productId()).isEqualTo(productId);
        assertThat(movement.locationId()).isEqualTo(sourceLocationId);
        assertThat(movement.destinationLocationId()).isEqualTo(destinationLocationId);
        assertThat(movement.type()).isEqualTo(MovementType.TRANSFER);
        assertThat(movement.quantity()).isEqualTo(5);
        assertThat(movement.executedBy()).isEqualTo(userId);
        assertThat(movement.executedAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 10, 0));
        assertThat(movement.saleId()).isNull();
    }

    /**
     * L'historique doit annoncer sous quelle forme la vente a été enregistrée : réglée, ou à
     * crédit et pour combien.
     *
     * <p>Les trois formes sont lues en une seule page, car c'est ainsi qu'elles se présentent à
     * l'écran — et parce que la résolution des soldes est justement groupée par page.</p>
     */
    @Test
    void should_announce_the_settlement_state_of_the_sale_behind_each_movement() {
        saveReferenceData();
        saveSale();

        // Vendue 91,80 €, dont 41,80 € versés au comptoir : il reste 50,00 €.
        Sale creditSale = Sale.create(
                userId,
                List.of(new SaleLineInput(productId, 2, unitPrice)),
                customerId,
                eur("41.80"));
        saleAdapter.save(creditSale);
        flushAndClear();

        StockMovement creditExit = StockMovement.createExit(
                productId, sourceLocationId, 2, userId, creditSale.getSaleId(), OperationId.generate());
        StockMovement paidExit = StockMovement.createExit(
                productId, sourceLocationId, 1, userId, sale.getSaleId(), OperationId.generate());
        StockMovement entry = StockMovement.createEntry(
                productId, destinationLocationId, 10, userId, OperationId.generate());

        adapter.saveAll(List.of(creditExit, paidExit, entry));
        flushAndClear();

        List<StockMovementView> movements = queryAdapter.findByQuery(allMovements()).content();

        // Vente à crédit : le solde restant, et non un simple drapeau.
        assertThat(viewOf(movements, creditExit).saleAmountDue()).isEqualTo(eur("50.00"));
        // Vente réglée : zéro, ce qui est une réponse, pas une absence de réponse.
        assertThat(viewOf(movements, paidExit).saleAmountDue()).isEqualTo(eur("0"));
        // Une réception ne naît d'aucune vente : il n'y a rien à annoncer.
        assertThat(viewOf(movements, entry).saleAmountDue()).isNull();
    }

    /**
     * Le cas le plus courant du terrain : le client emporte la marchandise sans rien verser. Aucune
     * ligne n'existe alors dans le ledger, et c'est la branche {@code COALESCE(..., 0)} de la
     * requête qui répond — celle qu'une jointure interne aurait silencieusement fait disparaître.
     */
    @Test
    void should_announce_the_full_total_when_nothing_was_paid_at_the_counter() {
        saveReferenceData();

        Sale unpaidSale = Sale.create(
                userId,
                List.of(new SaleLineInput(productId, 1, unitPrice)),
                customerId,
                Money.zero(Currency.getInstance("EUR")));
        saleAdapter.save(unpaidSale);
        flushAndClear();

        StockMovement exit = StockMovement.createExit(
                productId, sourceLocationId, 1, userId, unpaidSale.getSaleId(), OperationId.generate());
        adapter.saveAll(List.of(exit));
        flushAndClear();

        List<StockMovementView> movements = queryAdapter.findByQuery(allMovements()).content();

        assertThat(viewOf(movements, exit).saleAmountDue()).isEqualTo(eur("45.90"));
    }

    private static ListStockMovementsQuery allMovements() {
        return new ListStockMovementsQuery(
                0, 10, List.of("executedAt,asc"), null, null, null, null, null);
    }

    private static StockMovementView viewOf(List<StockMovementView> movements, StockMovement movement) {
        return movements.stream()
                .filter(view -> view.movementId().equals(movement.getMovementId()))
                .findFirst()
                .orElseThrow();
    }

    private static Money eur(String amount) {
        return Money.create(new BigDecimal(amount), Currency.getInstance("EUR"));
    }

    private void saveReferenceData() {
        customerRepository.save(CustomerJpaEntity.of(
                customerId.getValue(),
                "Moussa",
                "Youssouf",
                "+23566123456",
                null
        ));

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
                unitPrice.getCurrency().getCurrencyCode(),
                true
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
