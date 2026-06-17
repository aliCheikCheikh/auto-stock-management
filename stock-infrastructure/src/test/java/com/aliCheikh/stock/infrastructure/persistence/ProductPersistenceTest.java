package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.adapter.ProductJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.ProductJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
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
        ProductJpaRepositoryAdapter.class,
        ProductJpaMapper.class
})
class ProductPersistenceTest {

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
    private ProductJpaRepositoryAdapter adapter;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ProductId productId;
    private CategoryId categoryId;
    private Money unitPrice;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = ProductId.generate();
        categoryId = CategoryId.generate();
        unitPrice = Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"));
        product = new Product(
                productId,
                "Brake pads",
                "BRK-PAD-001",
                categoryId,
                5,
                unitPrice
        );
    }

    @Test
    void should_save_and_find_product_by_id() {
        saveCategory();
        saveProductAndClear();

        Optional<Product> foundProduct = adapter.findById(productId);

        assertPersistedProduct(foundProduct);
    }

    @Test
    void should_save_and_find_product_by_reference() {
        saveCategory();
        saveProductAndClear();

        Optional<Product> foundProduct = adapter.findByReference(product.getReference());

        assertPersistedProduct(foundProduct);
    }

    @Test
    void should_persist_inactive_product() {
        saveCategory();
        ProductId inactiveProductId = ProductId.generate();
        Product inactiveProduct = new Product(inactiveProductId,
                "Test Product",
                "Ref Test Product",
                categoryId,
                5,
                unitPrice);
        inactiveProduct.deactivate();
        adapter.save(inactiveProduct);
        flushAndClear();
        Optional<Product> foundProduct = adapter.findById(inactiveProductId);
        assertThat(foundProduct).isPresent();
        Product persistedProduct = foundProduct.orElseThrow();
        assertThat(persistedProduct.isActive()).isFalse();

    }

    @Test
    void should_return_only_active_products_when_finding_all_active() {
        saveCategory();

        Product activeProduct = new Product(
                ProductId.generate(), "active product", "PRD-001", categoryId, 5, unitPrice);
        Product inactiveProduct = new Product(
                ProductId.generate(), "inactive product", "PRD-002", categoryId, 5, unitPrice);
        inactiveProduct.deactivate();
        adapter.save(activeProduct);
        adapter.save(inactiveProduct);
        flushAndClear();

        List<Product> activeProducts = adapter.findAllActive(0, 10);

        assertThat(activeProducts)
                .extracting(Product::getReference)
                .containsExactly("PRD-001");
    }

    private void saveCategory() {
        categoryRepository.save(CategoryJpaEntity.of(
                categoryId.getValue(),
                "Brakes"
        ));
        flushAndClear();
    }

    private void saveProductAndClear() {
        adapter.save(product);
        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertPersistedProduct(Optional<Product> foundProduct) {
        assertThat(foundProduct).isPresent();
        Product persistedProduct = foundProduct.orElseThrow();
        assertThat(persistedProduct.getProductId()).isEqualTo(productId);
        assertThat(persistedProduct.getCategoryId()).isEqualTo(categoryId);
        assertThat(persistedProduct.getName()).isEqualTo("Brake pads");
        assertThat(persistedProduct.getReference()).isEqualTo("BRK-PAD-001");
        assertThat(persistedProduct.getUnitPrice()).isEqualTo(unitPrice);
    }
}
