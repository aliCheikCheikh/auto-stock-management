package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.ProductSearchView;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.adapter.ProductSearchQueryJpaAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProductSearchQueryJpaAdapter.class})
class ProductSearchPersistenceTest {

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

    private static final int LIMIT = 10;

    @Autowired
    private ProductSearchQueryJpaAdapter searchAdapter;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CategoryId categoryId;
    private final Money unitPrice = Money.create(new BigDecimal("2500.00"), Currency.getInstance("XAF"));

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        seedCatalog();
    }

    @Test
    void should_find_a_product_from_a_short_fragment_or_typo() {
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("filtr", LIMIT);

        assertThat(results)
                .extracting(ProductSearchView::name)
                .contains("Filtre à huile", "Filtre à air");
    }

    @Test
    void should_ignore_accents() {
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("filtre a huile", LIMIT);

        assertThat(results)
                .extracting(ProductSearchView::name)
                .contains("Filtre à huile");
    }

    @Test
    void should_find_a_product_from_a_partial_reference() {
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("brk-pad", LIMIT);

        assertThat(results)
                .extracting(ProductSearchView::reference)
                .contains("BRK-PAD-001");
    }

    @Test
    void should_find_products_from_a_very_short_reference_fragment() {
        // "flt" est trop court pour l'opérateur % (score dilué), mais doit remonter
        // par contenance (ILIKE) sur les références FLT-*.
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("flt", LIMIT);

        assertThat(results)
                .extracting(ProductSearchView::reference)
                .contains("FLT-HUI-001", "FLT-AIR-002");
    }

    @Test
    void should_only_return_active_products() {
        // "Filtre à gasoil" existe mais est inactif -> ne doit jamais remonter
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("gasoil", LIMIT);

        assertThat(results)
                .extracting(ProductSearchView::name)
                .doesNotContain("Filtre à gasoil");
    }

    @Test
    void should_rank_the_most_relevant_result_first() {
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("filtre a air", LIMIT);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).reference()).isEqualTo("FLT-AIR-002");
    }

    @Test
    void should_respect_the_limit() {
        // "filtre" matche les deux filtres actifs ; avec une limite de 1, on n'en veut qu'un
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("filtre", 1);

        assertThat(results).hasSize(1);
    }

    @Test
    void should_return_empty_list_when_nothing_matches() {
        List<ProductSearchView> results = searchAdapter.findProductsByKeyword("zzzzz", LIMIT);

        assertThat(results).isEmpty();
    }

    private void seedCatalog() {
        categoryRepository.save(CategoryJpaEntity.of(categoryId.getValue(), "Filtration-" + categoryId.getValue()));
        saveProduct("Filtre à huile", "FLT-HUI-001", true);
        saveProduct("Filtre à air", "FLT-AIR-002", true);
        saveProduct("Plaquette de frein", "BRK-PAD-001", true);
        saveProduct("Filtre à gasoil", "FLT-GAS-003", false);
        flushAndClear();
    }

    private void saveProduct(String name, String reference, boolean active) {
        productRepository.save(ProductJpaEntity.of(
                UUID.randomUUID(),
                name,
                reference,
                categoryId.getValue(),
                5,
                unitPrice.getAmount(),
                unitPrice.getCurrency().getCurrencyCode(),
                active
        ));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
