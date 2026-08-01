package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineInput;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.adapter.CreditSaleDetailQueryJpaAdapter;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le détail d'une créance, lu depuis la base.
 *
 * <p>Ce test existe surtout pour une raison : le détail est assemblé à partir de <b>trois</b>
 * requêtes plutôt qu'une seule jointure. Une jointure unique sur les lignes et les paiements
 * produirait un produit cartésien — deux produits et deux encaissements donneraient quatre lignes,
 * et le cumul encaissé serait doublé. Le cas est donc reproduit explicitement.</p>
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SaleJpaRepositoryAdapter.class,
        SaleJpaMapper.class,
        CreditSaleDetailQueryJpaAdapter.class})
class CreditSaleDetailPersistenceTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_credit_detail_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CreditSaleDetailQueryJpaAdapter detailAdapter;

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

    private CategoryId categoryId;
    private ProductId brakePadsId;
    private ProductId oilFilterId;
    private UserId sellerId;
    private CustomerId customerId;

    @BeforeEach
    void setUp() {
        categoryId = CategoryId.generate();
        brakePadsId = ProductId.generate();
        oilFilterId = ProductId.generate();
        sellerId = UserId.generate();
        customerId = CustomerId.generate();
    }

    @Test
    void should_describe_what_was_sold_by_whom_and_what_remains_due() {
        saveReferenceData();

        // 3 plaquettes à 40 000 + 2 filtres à 40 000 = 200 000, dont 100 000 versés au comptoir.
        Sale sale = Sale.create(
                sellerId,
                List.of(new SaleLineInput(brakePadsId, 3, xaf("40000")),
                        new SaleLineInput(oilFilterId, 2, xaf("40000"))),
                customerId,
                xaf("100000"));
        saleAdapter.save(sale);
        flushAndClear();

        // Un second encaissement : c'est ce couple deux lignes / deux paiements qui piégerait
        // une jointure unique.
        Sale reloaded = saleAdapter.findById(sale.getSaleId()).orElseThrow();
        reloaded.recordPayment(xaf("50000"), sellerId, LocalDateTime.now());
        saleAdapter.save(reloaded);
        flushAndClear();

        CreditSaleDetailView detail = detailAdapter
                .findCreditSaleDetail(sale.getSaleId().getValue())
                .orElseThrow();

        // « Quels produits ? » — nommés, pas des identifiants.
        assertThat(detail.lines()).hasSize(2);
        assertThat(detail.lines())
                .extracting(line -> line.productName() + " x" + line.quantity())
                .containsExactlyInAnyOrder("Plaquettes de frein x3", "Filtre à huile x2");

        // « Vendu par qui, à qui ? »
        assertThat(detail.sellerName()).isEqualTo("Ahmat");
        assertThat(detail.customerGivenName()).isEqualTo("Moussa");
        assertThat(detail.customerPhoneNumber()).isEqualTo("+23566123456");

        // « Combien payé, combien reste-t-il ? » Les montants ne doivent pas être doublés.
        assertThat(detail.totalAmount()).isEqualTo(xaf("200000"));
        assertThat(detail.amountPaid()).isEqualTo(xaf("150000"));
        assertThat(detail.amountDue()).isEqualTo(xaf("50000"));
        assertThat(detail.settled()).isFalse();

        // L'acompte du jour de la vente ouvre l'échéancier, le remboursement suit.
        assertThat(detail.payments()).hasSize(2);
        assertThat(detail.payments())
                .extracting(payment -> payment.amount())
                .containsExactly(xaf("100000"), xaf("50000"));
        assertThat(detail.payments().get(0).receivedByName()).isEqualTo("Ahmat");
    }

    @Test
    void should_report_a_fully_repaid_sale_as_settled() {
        saveReferenceData();

        Sale sale = Sale.create(
                sellerId,
                List.of(new SaleLineInput(brakePadsId, 1, xaf("40000"))),
                customerId,
                xaf("40000"));
        saleAdapter.save(sale);
        flushAndClear();

        CreditSaleDetailView detail = detailAdapter
                .findCreditSaleDetail(sale.getSaleId().getValue())
                .orElseThrow();

        assertThat(detail.settled()).isTrue();
        assertThat(detail.amountDue()).isEqualTo(xaf("0"));
    }

    /**
     * Le client emporte la marchandise sans rien verser : aucune ligne n'existe dans le ledger.
     *
     * <p>C'est la branche {@code COALESCE(..., 0)} de la requête d'en-tête. Une jointure interne
     * aux paiements aurait fait disparaître la créance entière — celle-là même qui compte le plus
     * pour le patron, puisque rien n'a été encaissé.</p>
     */
    @Test
    void should_describe_a_credit_sale_where_nothing_was_paid_at_the_counter() {
        saveReferenceData();

        Sale sale = Sale.create(
                sellerId,
                List.of(new SaleLineInput(brakePadsId, 2, xaf("40000"))),
                customerId,
                Money.zero(XAF));
        saleAdapter.save(sale);
        flushAndClear();

        CreditSaleDetailView detail = detailAdapter
                .findCreditSaleDetail(sale.getSaleId().getValue())
                .orElseThrow();

        assertThat(detail.lines()).hasSize(1);
        assertThat(detail.payments()).isEmpty();
        assertThat(detail.totalAmount()).isEqualTo(xaf("80000"));
        assertThat(detail.amountPaid()).isEqualTo(xaf("0"));
        assertThat(detail.amountDue()).isEqualTo(xaf("80000"));
        assertThat(detail.settled()).isFalse();
    }

    /** Une vente au comptant n'a pas de client : elle n'est pas une créance et n'a rien à exposer. */
    @Test
    void should_ignore_a_cash_sale() {
        saveReferenceData();

        Sale cashSale = Sale.create(sellerId, List.of(new SaleLineInput(brakePadsId, 1, xaf("40000"))));
        saleAdapter.save(cashSale);
        flushAndClear();

        assertThat(detailAdapter.findCreditSaleDetail(cashSale.getSaleId().getValue())).isEmpty();
    }

    @Test
    void should_return_nothing_for_an_unknown_sale() {
        Optional<CreditSaleDetailView> detail =
                detailAdapter.findCreditSaleDetail(SaleId.generate().getValue());

        assertThat(detail).isEmpty();
    }

    private void saveReferenceData() {
        categoryRepository.save(CategoryJpaEntity.of(categoryId.getValue(), "Freinage-test"));

        productRepository.save(ProductJpaEntity.of(
                brakePadsId.getValue(), "Plaquettes de frein", "BRK-PAD-001",
                categoryId.getValue(), 5, new BigDecimal("40000.00"), "XAF", true));

        productRepository.save(ProductJpaEntity.of(
                oilFilterId.getValue(), "Filtre à huile", "OIL-FILTER-001",
                categoryId.getValue(), 5, new BigDecimal("40000.00"), "XAF", true));

        userRepository.save(UserJpaEntity.of(sellerId.getValue(), "Ahmat", UserRole.SELLER));

        customerRepository.save(CustomerJpaEntity.of(
                customerId.getValue(), "Moussa", "Youssouf", "+23566123456", null));

        flushAndClear();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
