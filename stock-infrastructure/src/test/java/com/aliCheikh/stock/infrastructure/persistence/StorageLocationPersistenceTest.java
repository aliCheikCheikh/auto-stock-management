package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.infrastructure.persistence.adapter.StorageLocationJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ShopJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.StorageLocationJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ShopJpaRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        StorageLocationJpaRepositoryAdapter.class,
        StorageLocationJpaMapper.class
})
class StorageLocationPersistenceTest {

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
    private StorageLocationJpaRepositoryAdapter adapter;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private ShopJpaRepository shopRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ShopId shopId;
    private ProductId productId;
    private CategoryId categoryId;
    private LocationId locationId;
    private LocationId secondLocationId;
    private StorageLocation storageLocation;
    private StorageLocation secondStorageLocation;

    @BeforeEach
    void setUp() {
        shopId = ShopId.generate();
        productId = ProductId.generate();
        categoryId = CategoryId.generate();
        locationId = LocationId.generate();
        secondLocationId = LocationId.generate();

        storageLocation = new StorageLocation(
                locationId,
                shopId,
                LocationType.SHOP_FLOOR,
                "Rayon A",
                3
        );
        storageLocation.increaseStock(productId, 10);

        secondStorageLocation = new StorageLocation(
                secondLocationId,
                shopId,
                LocationType.BACKSTOCK,
                "Rayon B",
                3
        );
        secondStorageLocation.increaseStock(productId, 20);
    }

    @Test
    void should_save_and_find_storage_location_by_id_with_stock_levels() {
        saveReferenceData();
        saveStorageLocationAndClear();

        Optional<StorageLocation> found = adapter.findById(locationId);

        assertThat(found).isPresent();
        assertPersistedLocation(
                found.orElseThrow(),
                locationId,
                LocationType.SHOP_FLOOR,
                "Rayon A",
                10
        );
    }

    @Test
    void should_save_and_find_storage_locations_by_shop_id_with_stock_levels() {
        saveReferenceData();
        saveStorageLocationAndClear();

        List<StorageLocation> found = adapter.findByShopId(shopId);

        assertThat(found).hasSize(1);
        assertPersistedLocation(
                found.get(0),
                locationId,
                LocationType.SHOP_FLOOR,
                "Rayon A",
                10
        );
    }

    @Test
    void should_save_all_and_find_all_storage_locations_with_stock_levels() {
        saveReferenceData();

        adapter.saveAll(List.of(storageLocation, secondStorageLocation));
        flushAndClear();

        List<StorageLocation> found = adapter.findAll();

        assertThat(found).hasSize(2);
        assertThat(found)
                .extracting(StorageLocation::getLocationId)
                .containsExactlyInAnyOrder(locationId, secondLocationId);

        assertPersistedLocation(
                findByLocationId(found, locationId),
                locationId,
                LocationType.SHOP_FLOOR,
                "Rayon A",
                10
        );

        assertPersistedLocation(
                findByLocationId(found, secondLocationId),
                secondLocationId,
                LocationType.BACKSTOCK,
                "Rayon B",
                20
        );
    }

    private void saveReferenceData() {
        categoryRepository.save(CategoryJpaEntity.of(
                categoryId.getValue(),
                "Brakes"
        ));

        shopRepository.save(ShopJpaEntity.of(
                shopId.getValue(),
                "Nantes Shop",
                "42 Boulevard Gustave Roch"
        ));

        productRepository.save(ProductJpaEntity.of(
                productId.getValue(),
                "Brake pads",
                "BRK-PAD-001",
                categoryId.getValue(),
                5,
                new BigDecimal("45.90"),
                Currency.getInstance("EUR").getCurrencyCode()
        ));

        flushAndClear();
    }

    private void saveStorageLocationAndClear() {
        adapter.save(storageLocation);
        flushAndClear();
    }

    private StorageLocation findByLocationId(List<StorageLocation> locations, LocationId expectedLocationId) {
        return locations.stream()
                .filter(location -> location.getLocationId().equals(expectedLocationId))
                .findFirst()
                .orElseThrow();
    }

    private void assertPersistedLocation(
            StorageLocation location,
            LocationId expectedLocationId,
            LocationType expectedLocationType,
            String expectedLabel,
            int expectedQuantity
    ) {
        assertThat(location.getLocationId()).isEqualTo(expectedLocationId);
        assertThat(location.getShopId()).isEqualTo(shopId);
        assertThat(location.getLocationType()).isEqualTo(expectedLocationType);
        assertThat(location.getLabel()).isEqualTo(expectedLabel);
        assertThat(location.getLowStockIndicator()).isEqualTo(3);
        assertThat(location.getStockLevel(productId)).isEqualTo(expectedQuantity);
        assertThat(location.getStockLevels()).containsKey(productId);
        assertThat(location.getStockLevels().get(productId).getQuantity()).isEqualTo(expectedQuantity);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
