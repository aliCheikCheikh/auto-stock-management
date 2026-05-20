package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleLineInput;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.adapter.SaleJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.adapter.SaleQueryJpaAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.SaleJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
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
@Import({SaleJpaRepositoryAdapter.class,
        SaleJpaMapper.class,
        SaleQueryJpaAdapter.class})
public class SaleQueryPersistenceTest {

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
    private SaleQueryJpaAdapter queryAdapter;

    @Autowired
    private SaleJpaRepositoryAdapter adapter;

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
    private UserId otherSellerId;
    private UserId userId;
    private Money firstUnitPrice;
    private Money secondUnitPrice;
    private Sale sale;
    private Sale secondSale;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        firstProductId = ProductId.generate();
        secondProductId = ProductId.generate();
        sellerId = UserId.generate();
        otherSellerId = UserId.generate();
        firstUnitPrice = Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"));
        secondUnitPrice = Money.create(new BigDecimal("12.50"), Currency.getInstance("EUR"));
        sale = Sale.create(
                sellerId,
                List.of(
                        new SaleLineInput(firstProductId, 2, firstUnitPrice),
                        new SaleLineInput(secondProductId, 1, secondUnitPrice)
                )
        );
        secondSale = Sale.create(otherSellerId, List.of(
                new SaleLineInput(firstProductId, 5, firstUnitPrice),
                new SaleLineInput(secondProductId, 2, secondUnitPrice)
        ));
    }

    @Test
    void should_find_sales_by_query() {
        saveReferenceData();
        adapter.save(sale);
        flushAndClear();

        ListSalesQuery query = new ListSalesQuery(
                0,
                20,
                List.of("createdAt,desc"),
                null,
                null,
                null,
                null
        );

        PageResult<SaleView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1);

        SaleView view = result.content().get(0);
        assertThat(view.saleId()).isEqualTo(sale.getSaleId());
        assertThat(view.sellerId()).isEqualTo(sellerId);
        assertThat(view.lines()).hasSize(2);
        assertThat(view.totalAmount()).isEqualTo(sale.getTotalAmount());
        assertThat(view.createdAt()).isEqualTo(sale.getOccurredAt());

    }

    @Test
    void should_filter_sales_by_seller_id() {
        saveReferenceData();

        userRepository.save(UserJpaEntity.of(
                otherSellerId.getValue(),
                "other-seller",
                UserRole.SELLER
        ));
        flushAndClear();

        adapter.save(sale);
        adapter.save(secondSale);
        flushAndClear();

        ListSalesQuery query = new ListSalesQuery(
                0,
                20,
                List.of("createdAt,desc"),
                sellerId,
                null,
                null,
                null
        );

        PageResult<SaleView> result = queryAdapter.findByQuery(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content())
                .extracting(SaleView::saleId)
                .containsExactly(sale.getSaleId());
        assertThat(result.content().get(0).sellerId()).isEqualTo(sellerId);
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
                firstUnitPrice.getCurrency().getCurrencyCode()
        ));

        productRepository.save(ProductJpaEntity.of(
                secondProductId.getValue(),
                "Oil filter",
                "OIL-FILTER-001",
                categoryId.getValue(),
                5,
                secondUnitPrice.getAmount(),
                secondUnitPrice.getCurrency().getCurrencyCode()
        ));

        userRepository.save(UserJpaEntity.of(
                sellerId.getValue(),
                "seller",
                UserRole.SELLER
        ));

        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
