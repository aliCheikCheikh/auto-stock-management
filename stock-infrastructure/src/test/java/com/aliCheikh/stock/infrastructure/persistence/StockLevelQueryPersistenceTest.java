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
    private ShopId shopId;
    private LocationId locationId;
    private Money unitPrice;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        productId = ProductId.generate();
        shopId = ShopId.generate();
        locationId = LocationId.generate();
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
                5,
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
