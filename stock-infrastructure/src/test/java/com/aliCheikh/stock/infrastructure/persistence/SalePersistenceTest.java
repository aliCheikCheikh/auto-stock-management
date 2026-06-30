package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleLineInput;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.adapter.SaleJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.SaleLineJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.SaleJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.SaleJpaRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        SaleJpaRepositoryAdapter.class,
        SaleJpaMapper.class
})
class SalePersistenceTest {

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
    private SaleJpaRepositoryAdapter adapter;

    @Autowired
    private SaleJpaRepository saleRepository;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CategoryId categoryId;
    private ProductId firstProductId;
    private ProductId secondProductId;
    private UserId sellerId;
    private Money firstUnitPrice;
    private Money secondUnitPrice;
    private Sale sale;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        firstProductId = ProductId.generate();
        secondProductId = ProductId.generate();
        sellerId = UserId.generate();
        firstUnitPrice = Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"));
        secondUnitPrice = Money.create(new BigDecimal("12.50"), Currency.getInstance("EUR"));
        sale = Sale.create(
                sellerId,
                List.of(
                        new SaleLineInput(firstProductId, 2, firstUnitPrice),
                        new SaleLineInput(secondProductId, 1, secondUnitPrice)
                )
        );
    }

    @Test
    void should_save_sale_with_lines() {
        saveReferenceData();

        adapter.save(sale);
        flushAndClear();

        SaleJpaEntity persistedSale = saleRepository.findById(sale.getSaleId().getValue())
                .orElseThrow();

        assertThat(persistedSale.getId()).isEqualTo(sale.getSaleId().getValue());
        assertThat(persistedSale.getSoldBy()).isEqualTo(sellerId.getValue());
        assertThat(persistedSale.getOccurredAt()).isEqualTo(sale.getOccurredAt().truncatedTo(ChronoUnit.MICROS));
        assertThat(persistedSale.getTotalAmount()).isEqualByComparingTo(new BigDecimal("104.30"));
        assertThat(persistedSale.getTotalCurrency()).isEqualTo("EUR");
        assertThat(persistedSale.getSaleLines()).hasSize(2);

        assertPersistedLine(
                findLineByNumber(persistedSale, 1),
                firstProductId,
                2,
                new BigDecimal("45.90"),
                new BigDecimal("91.80")
        );
        assertPersistedLine(
                findLineByNumber(persistedSale, 2),
                secondProductId,
                1,
                new BigDecimal("12.50"),
                new BigDecimal("12.50")
        );
    }

    private void saveReferenceData() {
        categoryRepository.save(CategoryJpaEntity.of(
                categoryId.getValue(),
                "Brakes"
        ));

        productRepository.save(ProductJpaEntity.of(
                firstProductId.getValue(),
                "Brake pads",
                "BRK-PAD-001",
                categoryId.getValue(),
                5,
                firstUnitPrice.getAmount(),
                firstUnitPrice.getCurrency().getCurrencyCode(),
                true
        ));

        productRepository.save(ProductJpaEntity.of(
                secondProductId.getValue(),
                "Oil filter",
                "OIL-FILTER-001",
                categoryId.getValue(),
                5,
                secondUnitPrice.getAmount(),
                secondUnitPrice.getCurrency().getCurrencyCode(),
                true
        ));

        userRepository.save(UserJpaEntity.of(
                sellerId.getValue(),
                "seller",
                UserRole.SELLER
        ));

        flushAndClear();
    }

    private SaleLineJpaEntity findLineByNumber(SaleJpaEntity sale, int lineNumber) {
        return sale.getSaleLines()
                .stream()
                .filter(line -> line.getLineNumber() == lineNumber)
                .findFirst()
                .orElseThrow();
    }

    private void assertPersistedLine(
            SaleLineJpaEntity line,
            ProductId expectedProductId,
            int expectedQuantity,
            BigDecimal expectedUnitPrice,
            BigDecimal expectedLineTotal
    ) {
        assertThat(line.getSaleId()).isEqualTo(sale.getSaleId().getValue());
        assertThat(line.getProductId()).isEqualTo(expectedProductId.getValue());
        assertThat(line.getQuantity()).isEqualTo(expectedQuantity);
        assertThat(line.getUnitPriceAmount()).isEqualByComparingTo(expectedUnitPrice);
        assertThat(line.getUnitPriceCurrency()).isEqualTo("EUR");
        assertThat(line.getLineTotalAmount()).isEqualByComparingTo(expectedLineTotal);
        assertThat(line.getLineTotalCurrency()).isEqualTo("EUR");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
