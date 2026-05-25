package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.dto.ProductStockSummaryView;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ShopJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockLevelJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ShopJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.StorageLocationJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.adapter.ProductStockQueryJpaAdapter;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ProductStockQueryJpaAdapter.class
})
class ProductStockQueryPersistenceTest {
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
    private ProductStockQueryJpaAdapter queryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private StorageLocationJpaRepository storageLocationRepository;

    @Autowired
    private ShopJpaRepository shopRepository;

    private CategoryId categoryId;
    private ProductId productId;
    private ShopId shopId;
    private LocationId locationId;
    private LocationId otherLocationId;
    private Money unitPrice;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        productId = ProductId.generate();
        shopId = ShopId.generate();
        locationId = LocationId.generate();
        otherLocationId = LocationId.generate();
        unitPrice = Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR"));
    }

    @Test
    void should_find_product_stock_summary() {
        saveReferenceData();
        StorageLocationJpaEntity location = StorageLocationJpaEntity.of(
                locationId.getValue(),
                shopId.getValue(),
                LocationType.SHOP_FLOOR,
                "Shop floor",
                3
        );


        location.replaceStockLevels(
                Set.of(StockLevelJpaEntity.of(location, productId.getValue(), 3))
        );

        storageLocationRepository.save(location);

        StorageLocationJpaEntity otherLocation = StorageLocationJpaEntity.of(
                otherLocationId.getValue(),
                shopId.getValue(),
                LocationType.BACKSTOCK,
                "Backstock",
                3
        );

        otherLocation.replaceStockLevels(
                Set.of(StockLevelJpaEntity.of(otherLocation, productId.getValue(), 4))
        );

        storageLocationRepository.save(otherLocation);
        flushAndClear();

        GetProductStockLevelsQuery query = new GetProductStockLevelsQuery(
                productId,
                null
        );

        Optional<ProductStockSummaryView> result = queryAdapter.findProductStockSummary(query);

        assertThat(result).isPresent();

        ProductStockSummaryView summary = result.orElseThrow();
        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.productName()).isEqualTo("Oil Filter");
        assertThat(summary.globalQuantity()).isEqualTo(7);
        assertThat(summary.minimumGlobalThreshold()).isEqualTo(10);
        assertThat(summary.belowGlobalThreshold()).isTrue();
        assertThat(summary.byLocation()).hasSize(2);
        assertThat(summary.byLocation())
                .extracting(StockLevelView::locationName)
                .containsExactlyInAnyOrder("Shop floor", "Backstock");
        assertThat(summary.byLocation())
                .extracting(StockLevelView::locationId)
                .containsExactlyInAnyOrder(locationId, otherLocationId);
        assertThat(summary.byLocation())
                .extracting(StockLevelView::quantity)
                .containsExactlyInAnyOrder(3, 4);


    }

    @Test
    void should_return_empty_stock_summary_when_product_has_no_stock_levels() {
        saveReferenceData();

        GetProductStockLevelsQuery query = new GetProductStockLevelsQuery(
                productId,
                null
        );

        Optional<ProductStockSummaryView> result = queryAdapter.findProductStockSummary(query);

        assertThat(result).isPresent();

        ProductStockSummaryView summary = result.orElseThrow();
        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.productName()).isEqualTo("Oil Filter");
        assertThat(summary.globalQuantity()).isZero();
        assertThat(summary.minimumGlobalThreshold()).isEqualTo(10);
        assertThat(summary.belowGlobalThreshold()).isTrue();
        assertThat(summary.byLocation()).isEmpty();
    }

    @Test
    void should_return_empty_when_product_does_not_exist() {
        GetProductStockLevelsQuery query = new GetProductStockLevelsQuery(
                productId,
                null
        );

        Optional<ProductStockSummaryView> result = queryAdapter.findProductStockSummary(query);

        assertThat(result).isEmpty();
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
                unitPrice.getCurrency().getCurrencyCode()
        ));

        shopRepository.save(ShopJpaEntity.of(shopId.getValue(),
                "Nantes Shop",
                "42 Boulevard Gustave Roch"));

        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
