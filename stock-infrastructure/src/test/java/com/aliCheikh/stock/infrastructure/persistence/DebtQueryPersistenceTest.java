package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.DebtStatus;
import com.aliCheikh.stock.application.dto.DebtView;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Payment;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.adapter.DebtQueryJpaAdapter;
import com.aliCheikh.stock.infrastructure.persistence.adapter.SaleJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.CustomerJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.SaleJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.CustomerJpaRepository;
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
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies PostgreSQL debt status filters, ordering, aggregated sale counts and nullable customer
 * parameters.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SaleJpaRepositoryAdapter.class, SaleJpaMapper.class, DebtQueryJpaAdapter.class})
class DebtQueryPersistenceTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    /** Fixed timestamps keep ordering independent of the test execution time. */
    private static final LocalDateTime REFERENCE = LocalDateTime.of(2026, 7, 28, 12, 0);

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_debt_query_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DebtQueryJpaAdapter debtAdapter;

    @Autowired
    private SaleJpaRepositoryAdapter saleAdapter;

    @Autowired
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private CustomerJpaRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ProductId productId;
    private UserId sellerId;
    private CustomerId moussaId;
    private CustomerId fatimeId;

    private SaleId oldUnpaidByMoussa;
    private SaleId settledLongAgoByMoussa;
    private SaleId neverPaidByFatime;
    private SaleId recentlySettledByFatime;

    @BeforeEach
    void setUp() {
        CategoryId categoryId = CategoryId.generate();
        productId = ProductId.generate();
        sellerId = UserId.generate();
        moussaId = CustomerId.generate();
        fatimeId = CustomerId.generate();

        categoryRepository.save(CategoryJpaEntity.of(categoryId.getValue(), "Freinage-test"));
        productRepository.save(ProductJpaEntity.of(
                productId.getValue(), "Plaquettes de frein", "BRK-PAD-001",
                categoryId.getValue(), 5, new BigDecimal("10000.00"), "XAF", true));
        userRepository.save(UserJpaEntity.of(sellerId.getValue(), "Ahmat", UserRole.SELLER));
        customerRepository.save(CustomerJpaEntity.of(
                moussaId.getValue(), "Moussa", "Youssouf", "+23566123456", null));
        customerRepository.save(CustomerJpaEntity.of(
                fatimeId.getValue(), "Fatimé", "Abakar", "+23566654321", null));
        flushAndClear();

        // Older debt: total 200,000, amount paid 50,000.
        oldUnpaidByMoussa = saveSale(REFERENCE.minusDays(60), moussaId, 20,
                payment("50000", REFERENCE.minusDays(55)));

        // An older settlement.
        settledLongAgoByMoussa = saveSale(REFERENCE.minusDays(30), moussaId, 10,
                payment("100000", REFERENCE.minusDays(25)));

        // No initial payment or ledger entry.
        neverPaidByFatime = saveSale(REFERENCE.minusDays(10), fatimeId, 8);

        // A recent settlement.
        recentlySettledByFatime = saveSale(REFERENCE.minusDays(5), fatimeId, 4,
                payment("40000", REFERENCE.minusDays(2)));

        // Cash sales without customers are excluded from debts.
        saveSale(REFERENCE.minusDays(1), null, 2, payment("20000", REFERENCE.minusDays(1)));

        flushAndClear();
    }

    @Test
    void should_list_open_debts_from_the_oldest_to_the_most_recent() {
        PageResult<DebtView> page = find(DebtStatus.OUTSTANDING, null, 0, 20);

        // Oldest outstanding debt first.
        assertThat(page.content()).extracting(DebtView::saleId)
                .containsExactly(oldUnpaidByMoussa.getValue(), neverPaidByFatime.getValue());
        assertThat(page.totalElements()).isEqualTo(2);

        DebtView oldest = page.content().get(0);
        assertThat(oldest.totalAmount()).isEqualTo(xaf("200000"));
        assertThat(oldest.amountPaid()).isEqualTo(xaf("50000"));
        assertThat(oldest.amountDue()).isEqualTo(xaf("150000"));
        assertThat(oldest.lastPaymentAt()).isEqualTo(REFERENCE.minusDays(55));
        assertThat(oldest.customerGivenName()).isEqualTo("Moussa");

        // A debt with no payments remains visible with no latest-payment timestamp.
        DebtView neverPaid = page.content().get(1);
        assertThat(neverPaid.lastPaymentAt()).isNull();
        assertThat(neverPaid.amountPaid()).isEqualTo(xaf("0"));
        assertThat(neverPaid.amountDue()).isEqualTo(xaf("80000"));
    }

    /** Settled debts remain in history. */
    @Test
    void should_keep_settled_debts_readable_from_the_latest_settlement() {
        PageResult<DebtView> page = find(DebtStatus.SETTLED, null, 0, 20);

        assertThat(page.content()).extracting(DebtView::saleId)
                .containsExactly(recentlySettledByFatime.getValue(), settledLongAgoByMoussa.getValue());
        assertThat(page.totalElements()).isEqualTo(2);

        DebtView latest = page.content().get(0);
        assertThat(latest.amountDue()).isEqualTo(xaf("0"));
        // Order by settlement date rather than sale date.
        assertThat(latest.lastPaymentAt()).isEqualTo(REFERENCE.minusDays(2));
    }

    @Test
    void should_mix_both_states_from_the_most_recent_sale() {
        PageResult<DebtView> page = find(DebtStatus.ALL, null, 0, 20);

        assertThat(page.content()).extracting(DebtView::saleId)
                .containsExactly(
                        recentlySettledByFatime.getValue(),
                        neverPaidByFatime.getValue(),
                        settledLongAgoByMoussa.getValue(),
                        oldUnpaidByMoussa.getValue());
        // Cash sales without customers remain excluded.
        assertThat(page.totalElements()).isEqualTo(4);
    }

    @Test
    void should_restrict_to_one_customer_without_changing_anything_else() {
        assertThat(find(DebtStatus.ALL, moussaId, 0, 20).content())
                .extracting(DebtView::saleId)
                .containsExactly(settledLongAgoByMoussa.getValue(), oldUnpaidByMoussa.getValue());

        assertThat(find(DebtStatus.SETTLED, moussaId, 0, 20).content())
                .extracting(DebtView::saleId)
                .containsExactly(settledLongAgoByMoussa.getValue());

        assertThat(find(DebtStatus.OUTSTANDING, fatimeId, 0, 20).content())
                .extracting(DebtView::saleId)
                .containsExactly(neverPaidByFatime.getValue());
    }

    /** Count aggregated sales rather than payment rows to preserve pagination totals. */
    @Test
    void should_count_sales_and_not_payments_when_paginating() {
        PageResult<DebtView> firstPage = find(DebtStatus.ALL, null, 0, 2);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(4);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.page()).isZero();

        PageResult<DebtView> secondPage = find(DebtStatus.ALL, null, 1, 2);

        assertThat(secondPage.content()).extracting(DebtView::saleId)
                .containsExactly(settledLongAgoByMoussa.getValue(), oldUnpaidByMoussa.getValue());
        assertThat(secondPage.totalElements()).isEqualTo(4);

        // Pages must not overlap.
        assertThat(firstPage.content()).extracting(DebtView::saleId)
                .doesNotContainAnyElementsOf(
                        secondPage.content().stream().map(DebtView::saleId).toList());
    }

    @Test
    void should_return_an_empty_page_for_a_customer_who_owes_nothing() {
        CustomerId unknown = CustomerId.generate();

        PageResult<DebtView> page = find(DebtStatus.ALL, unknown, 0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    private PageResult<DebtView> find(DebtStatus status, CustomerId customerId, int page, int size) {
        return debtAdapter.findByQuery(new ListDebtsQuery(
                page, size, status, customerId == null ? null : customerId.getValue()));
    }

    private SaleId saveSale(LocalDateTime occurredAt,
                            CustomerId customerId,
                            int quantity,
                            Payment... payments) {
        Money unitPrice = xaf("10000");
        Money lineTotal = unitPrice.multiply(quantity);
        SaleId saleId = SaleId.generate();

        saleAdapter.save(Sale.rehydrate(
                saleId,
                sellerId,
                occurredAt,
                lineTotal,
                List.of(new SaleLineDto(productId, quantity, unitPrice, lineTotal)),
                customerId,
                List.of(payments)));
        flushAndClear();

        return saleId;
    }

    private Payment payment(String amount, LocalDateTime receivedAt) {
        return Payment.record(xaf(amount), sellerId, receivedAt);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
