package com.aliCheikh.stock.infrastructure.persistence;


import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.infrastructure.persistence.adapter.StockLevelQueryJpaAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ShopJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockLevelJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ShopJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.StorageLocationJpaRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        StockLevelQueryJpaAdapter.class
})
class StockLevelQueryPersistenceTest {

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
    private StockLevelQueryJpaAdapter queryAdapter;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private ShopJpaRepository shopRepository;

    @Autowired
    private StorageLocationJpaRepository storageLocationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CategoryId categoryId;
    private ProductId productId;
    private ProductId secondProductId;
    private ShopId shopId;
    private ShopId otherShopId;
    private LocationId locationId;
    private LocationId otherLocationId;
    private Money unitPrice;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        productId = ProductId.generate();
        secondProductId = ProductId.generate();
        shopId = ShopId.generate();
        otherShopId = ShopId.generate();
        locationId = LocationId.generate();
        otherLocationId = LocationId.generate();
        unitPrice = Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR"));
    }

    @Test
    void should_find_stock_levels_by_query() {
        saveReferenceData();

        StorageLocationJpaEntity location = StorageLocationJpaEntity.of(
                locationId.getValue(),
                shopId.getValue(),
                LocationType.SHOP_FLOOR,
                "Shop floor",
                3
        );

        location.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(location, productId.getValue(), 7)
        ));

        storageLocationRepository.save(location);

        flushAndClear();

        ListStockLevelsQuery query = new ListStockLevelsQuery(
                0,
                20,
                null,
                null,
                null,
                false
        );

        PageResult<StockLevelView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);

        StockLevelView stockLevel = result.content().get(0);
        assertThat(stockLevel.productId()).isEqualTo(productId);
        assertThat(stockLevel.productName()).isEqualTo("Oil Filter");
        assertThat(stockLevel.locationId()).isEqualTo(locationId);
        assertThat(stockLevel.locationName()).isEqualTo("Shop floor");
        assertThat(stockLevel.locationType()).isEqualTo(LocationType.SHOP_FLOOR);
        assertThat(stockLevel.shopId()).isEqualTo(shopId);
        assertThat(stockLevel.quantity()).isEqualTo(7);
    }

    @Test
    void should_filter_stock_levels_by_product_id() {
        saveReferenceData();
        saveProduct(secondProductId, "Brake Pads", "BRK-PAD-001", 5);

        StorageLocationJpaEntity location = StorageLocationJpaEntity.of(
                locationId.getValue(),
                shopId.getValue(),
                LocationType.SHOP_FLOOR,
                "Shop floor",
                3
        );

        location.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(location, productId.getValue(), 7),
                StockLevelJpaEntity.of(location, secondProductId.getValue(), 12)
        ));

        storageLocationRepository.save(location);
        flushAndClear();

        ListStockLevelsQuery query = new ListStockLevelsQuery(
                0,
                20,
                productId,
                null,
                null,
                false
        );

        PageResult<StockLevelView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content())
                .extracting(StockLevelView::productId)
                .containsExactly(productId);
    }

    @Test
    void should_filter_stock_levels_by_shop_id() {
        saveReferenceData();
        saveShop(otherShopId, "Rennes Shop");

        StorageLocationJpaEntity matchingLocation = StorageLocationJpaEntity.of(
                locationId.getValue(),
                shopId.getValue(),
                LocationType.SHOP_FLOOR,
                "Nantes shop floor",
                3
        );
        matchingLocation.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(matchingLocation, productId.getValue(), 7)
        ));

        StorageLocationJpaEntity otherLocation = StorageLocationJpaEntity.of(
                otherLocationId.getValue(),
                otherShopId.getValue(),
                LocationType.BACKSTOCK,
                "Rennes backstock",
                3
        );
        otherLocation.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(otherLocation, productId.getValue(), 11)
        ));

        storageLocationRepository.save(matchingLocation);
        storageLocationRepository.save(otherLocation);
        flushAndClear();

        ListStockLevelsQuery query = new ListStockLevelsQuery(
                0,
                20,
                null,
                shopId,
                null,
                false
        );

        PageResult<StockLevelView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content())
                .extracting(StockLevelView::locationId)
                .containsExactly(locationId);
        assertThat(result.content().get(0).shopId()).isEqualTo(shopId);
    }

    @Test
    void should_filter_stock_levels_by_location_id() {
        saveReferenceData();

        StorageLocationJpaEntity matchingLocation = StorageLocationJpaEntity.of(
                locationId.getValue(),
                shopId.getValue(),
                LocationType.SHOP_FLOOR,
                "Shop floor",
                3
        );
        matchingLocation.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(matchingLocation, productId.getValue(), 7)
        ));

        StorageLocationJpaEntity otherLocation = StorageLocationJpaEntity.of(
                otherLocationId.getValue(),
                shopId.getValue(),
                LocationType.BACKSTOCK,
                "Backstock",
                3
        );
        otherLocation.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(otherLocation, productId.getValue(), 11)
        ));

        storageLocationRepository.save(matchingLocation);
        storageLocationRepository.save(otherLocation);
        flushAndClear();

        ListStockLevelsQuery query = new ListStockLevelsQuery(
                0,
                20,
                null,
                null,
                locationId,
                false
        );

        PageResult<StockLevelView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content())
                .extracting(StockLevelView::locationId)
                .containsExactly(locationId);
    }

    @Test
    void should_filter_stock_levels_below_global_threshold() {
        saveReferenceData();
        saveProduct(secondProductId, "Brake Pads", "BRK-PAD-001", 10);

        StorageLocationJpaEntity shopFloor = StorageLocationJpaEntity.of(
                locationId.getValue(),
                shopId.getValue(),
                LocationType.SHOP_FLOOR,
                "Shop floor",
                3
        );
        shopFloor.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(shopFloor, productId.getValue(), 3),
                StockLevelJpaEntity.of(shopFloor, secondProductId.getValue(), 8)
        ));

        StorageLocationJpaEntity backstock = StorageLocationJpaEntity.of(
                otherLocationId.getValue(),
                shopId.getValue(),
                LocationType.BACKSTOCK,
                "Backstock",
                3
        );
        backstock.replaceStockLevels(Set.of(
                StockLevelJpaEntity.of(backstock, productId.getValue(), 4),
                StockLevelJpaEntity.of(backstock, secondProductId.getValue(), 7)
        ));

        storageLocationRepository.save(shopFloor);
        storageLocationRepository.save(backstock);
        flushAndClear();

        ListStockLevelsQuery query = new ListStockLevelsQuery(
                0,
                20,
                null,
                null,
                null,
                true
        );

        PageResult<StockLevelView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content())
                .extracting(StockLevelView::productId)
                .containsOnly(productId);
        assertThat(result.content())
                .extracting(StockLevelView::quantity)
                .containsExactlyInAnyOrder(3, 4);
    }

    private void saveReferenceData() {
        categoryRepository.save(CategoryJpaEntity.of(
                categoryId.getValue(),
                "Filters"
        ));

        productRepository.save(ProductJpaEntity.of(
                productId.getValue(),
                "Oil Filter",
                "OIL-FILTER-001",
                categoryId.getValue(),
                10,
                unitPrice.getAmount(),
                unitPrice.getCurrency().getCurrencyCode(),
                true
        ));

        shopRepository.save(ShopJpaEntity.of(shopId.getValue(),
                "Nantes Shop",
                "42 Boulevard Gustave Roch"));

        flushAndClear();
    }

    private void saveProduct(ProductId productId, String name, String reference, int minimumGlobalThreshold) {
        productRepository.save(ProductJpaEntity.of(
                productId.getValue(),
                name,
                reference,
                categoryId.getValue(),
                minimumGlobalThreshold,
                unitPrice.getAmount(),
                unitPrice.getCurrency().getCurrencyCode(),
                true
        ));
    }

    private void saveShop(ShopId shopId, String name) {
        shopRepository.save(ShopJpaEntity.of(
                shopId.getValue(),
                name,
                "1 Test Street"
        ));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
