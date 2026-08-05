package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class StockReceiptImportLookupPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_import_lookup_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductJpaRepository productRepository;
    @Autowired
    private CategoryJpaRepository categoryRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void resolves_import_candidates_case_insensitively_with_postgresql() {
        UUID categoryId = UUID.randomUUID();
        String categoryName = "Import-Freinage-" + categoryId;
        categoryRepository.save(CategoryJpaEntity.of(categoryId, categoryName));
        ProductJpaEntity product = ProductJpaEntity.of(
                UUID.randomUUID(),
                "Filtre Import",
                "REF-IMPORT-001",
                categoryId,
                2,
                new BigDecimal("12500.00"),
                "XAF",
                true
        );
        productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        assertThat(productRepository.findByNormalizedReferences(Set.of("ref-import-001")))
                .extracting(ProductJpaEntity::getId)
                .containsExactly(product.getId());
        assertThat(productRepository.findByNormalizedNames(Set.of("filtre import")))
                .extracting(ProductJpaEntity::getId)
                .containsExactly(product.getId());
        assertThat(categoryRepository.findByNormalizedNames(Set.of(categoryName.toLowerCase())))
                .extracting(CategoryJpaEntity::getId)
                .containsExactly(categoryId);
    }
}
