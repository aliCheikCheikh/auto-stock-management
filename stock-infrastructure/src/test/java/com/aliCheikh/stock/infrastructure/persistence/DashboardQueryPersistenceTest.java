package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.dashboard.DashboardQueryCriteria;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSnapshot;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;
import com.aliCheikh.stock.infrastructure.persistence.adapter.DashboardQuerySqlAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DashboardQuerySqlAdapter.class)
class DashboardQueryPersistenceTest {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 10, 0);

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
    private DashboardQuerySqlAdapter queryAdapter;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID categoryId;
    private UUID shopId;
    private UUID locationId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        locationId = UUID.randomUUID();
        sellerId = UUID.randomUUID();

        jdbc.update("INSERT INTO category (id, name) VALUES (?, ?)", categoryId, unique("Dashboard"));
        jdbc.update("INSERT INTO shop (id, name, address) VALUES (?, ?, ?)",
                shopId, "Boutique test", "N'Djamena");
        jdbc.update("""
                INSERT INTO storage_location
                    (id, shop_id, location_type, label, low_stock_indicator, version)
                VALUES (?, ?, 'SHOP_FLOOR', 'Surface', 2, 0)
                """, locationId, shopId);
        jdbc.update("""
                INSERT INTO app_user
                    (id, username, display_name, role, email, password_temporary, active)
                VALUES (?, ?, 'Amina', 'OWNER', ?, FALSE, TRUE)
                """, sellerId, unique("amina"), unique("amina") + "@test.local");
    }

    @Test
    void should_count_and_prioritize_actionable_stock_alerts() {
        UUID noStock = product("Rupture sans ligne", "RUP-0", 4, true);
        UUID empty = product("Rupture", "RUP-1", 5, true);
        UUID low = product("Stock faible", "LOW-1", 6, true);
        UUID atThreshold = product("Au seuil", "OK-1", 5, true);
        UUID inactive = product("Produit inactif", "OFF-1", 10, false);

        stock(empty, 0);
        stock(low, 2);
        stock(atThreshold, 5);
        stock(inactive, 0);

        DashboardSnapshot result = queryAdapter.load(criteria());

        assertThat(result.stock().outOfStockCount()).isEqualTo(2);
        assertThat(result.stock().belowThresholdCount()).isEqualTo(1);
        assertThat(result.stock().alerts())
                .extracting(DashboardSummary.StockAlert::productId)
                .containsExactly(empty, noStock, low);
        assertThat(result.stock().alerts().get(0).status())
                .isEqualTo(DashboardSummary.StockAlertStatus.OUT_OF_STOCK);
        assertThat(result.stock().alerts().get(2).shortage()).isEqualTo(4);
    }

    @Test
    void should_calculate_sales_periods_without_multiplying_sale_totals_by_lines() {
        UUID oilFilter = product("Filtre à huile", "FIL-1", 0, true);
        UUID brakePads = product("Plaquettes", "PLA-1", 0, true);

        UUID todaySale = sale(NOW.minusHours(1), "50000");
        saleLine(todaySale, 1, oilFilter, 2, "20000");
        saleLine(todaySale, 2, brakePads, 1, "10000");

        UUID recentSale = sale(NOW.minusDays(2), "30000");
        saleLine(recentSale, 1, oilFilter, 3, "10000");

        UUID previousSale = sale(NOW.minusDays(8), "20000");
        saleLine(previousSale, 1, brakePads, 2, "10000");

        UUID oldSale = sale(NOW.minusDays(20), "90000");
        saleLine(oldSale, 1, oilFilter, 9, "10000");

        DashboardSummary.SalesSummary sales = queryAdapter.load(criteria()).sales();

        assertThat(sales.today().saleCount()).isEqualTo(1);
        assertThat(sales.today().itemsSold()).isEqualTo(3);
        assertThat(sales.today().revenue().getAmount()).isEqualByComparingTo("50000");
        assertThat(sales.today().averageBasket().getAmount()).isEqualByComparingTo("50000");

        assertThat(sales.last7Days().saleCount()).isEqualTo(2);
        assertThat(sales.last7Days().itemsSold()).isEqualTo(6);
        assertThat(sales.last7Days().revenue().getAmount()).isEqualByComparingTo("80000");
        assertThat(sales.last7Days().averageBasket().getAmount()).isEqualByComparingTo("40000");
        assertThat(sales.revenueChangePercent()).isEqualByComparingTo("300.00");

        assertThat(sales.bestSeller().productId()).isEqualTo(oilFilter);
        assertThat(sales.bestSeller().quantitySold()).isEqualTo(5);
        assertThat(sales.topProducts())
                .extracting(DashboardSummary.TopProduct::productId)
                .containsExactly(oilFilter, brakePads);
    }

    @Test
    void should_aggregate_open_debts_and_exclude_settled_sales() {
        UUID overdueCustomer = customer("Moussa", "Mahamat", "+23566000001");
        UUID recentCustomer = customer("Amina", null, "+23566000002");
        UUID settledCustomer = customer("Saleh", null, "+23566000003");

        UUID oldestSale = creditSale(overdueCustomer, NOW.minusDays(40), "200000");
        payment(oldestSale, NOW.minusDays(40), "50000");
        payment(oldestSale, NOW.minusDays(5), "10000");

        UUID secondOpenSale = creditSale(overdueCustomer, NOW.minusDays(20), "30000");
        UUID recentSale = creditSale(recentCustomer, NOW.minusDays(10), "100000");
        payment(recentSale, NOW.minusDays(10), "20000");

        UUID settledSale = creditSale(settledCustomer, NOW.minusDays(50), "50000");
        payment(settledSale, NOW.minusDays(49), "50000");

        DashboardSummary.DebtSummary debts = queryAdapter.load(criteria()).debts();

        assertThat(debts.openDebtCount()).isEqualTo(3);
        assertThat(debts.openCustomerCount()).isEqualTo(2);
        assertThat(debts.overdueCustomerCount()).isEqualTo(1);
        assertThat(debts.totalOutstanding().getAmount()).isEqualByComparingTo("250000");

        assertThat(debts.aging())
                .extracting(DashboardSummary.DebtAging::debtCount)
                .containsExactly(0L, 1L, 1L, 1L);
        assertThat(debts.aging())
                .extracting(aging -> aging.amountDue().getAmount())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(
                        new BigDecimal("0"),
                        new BigDecimal("80000"),
                        new BigDecimal("30000"),
                        new BigDecimal("140000"));

        assertThat(debts.customersToContact()).hasSize(1);
        DashboardSummary.CustomerDebtAlert alert = debts.customersToContact().get(0);
        assertThat(alert.customerId()).isEqualTo(overdueCustomer);
        assertThat(alert.openDebtCount()).isEqualTo(2);
        assertThat(alert.totalDue().getAmount()).isEqualByComparingTo("170000");
        assertThat(alert.oldestSaleId()).isEqualTo(oldestSale);
        assertThat(alert.daysOutstanding()).isEqualTo(40);

        // Use explicit IDs for intentionally excluded fixtures.
        assertThat(secondOpenSale).isNotEqualTo(oldestSale);
        assertThat(settledSale).isNotEqualTo(recentSale);
    }

    @Test
    void should_group_recent_operations_and_not_duplicate_the_initial_payment() {
        UUID firstProduct = product("Alternateur", "ALT-1", 0, true);
        UUID secondProduct = product("Démarreur", "DEM-1", 0, true);
        UUID customerId = customer("Fatime", null, "+23566000004");

        UUID saleId = creditSale(customerId, NOW.minusMinutes(5), "50000");
        saleLine(saleId, 1, firstProduct, 2, "20000");
        saleLine(saleId, 2, secondProduct, 1, "10000");
        payment(saleId, NOW.minusMinutes(5), "10000");
        payment(saleId, NOW.minusMinutes(1), "5000");
        movement(firstProduct, "EXIT", locationId, null, 2,
                NOW.minusMinutes(5), saleId, saleId);

        UUID receiptOperation = UUID.randomUUID();
        movement(firstProduct, "ENTRY", null, locationId, 3,
                NOW.minusMinutes(10), null, receiptOperation);
        movement(secondProduct, "ENTRY", null, locationId, 4,
                NOW.minusMinutes(10), null, receiptOperation);

        UUID reserveId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO storage_location
                    (id, shop_id, location_type, label, low_stock_indicator, version)
                VALUES (?, ?, 'BACKSTOCK', 'Réserve', 0, 0)
                """, reserveId, shopId);
        UUID transferOperation = UUID.randomUUID();
        movement(firstProduct, "TRANSFER", reserveId, locationId, 6,
                NOW.minusMinutes(20), null, transferOperation);

        var activities = queryAdapter.load(criteria()).recentActivity();

        assertThat(activities)
                .extracting(DashboardSummary.RecentActivity::type)
                .containsExactly(
                        DashboardSummary.ActivityType.DEBT_PAYMENT,
                        DashboardSummary.ActivityType.SALE,
                        DashboardSummary.ActivityType.STOCK_RECEIPT,
                        DashboardSummary.ActivityType.STOCK_TRANSFER);
        assertThat(activities).hasSize(4);

        DashboardSummary.RecentActivity sale = activities.get(1);
        assertThat(sale.resourceId()).isEqualTo(saleId);
        assertThat(sale.itemCount()).isEqualTo(2);
        assertThat(sale.quantity()).isEqualTo(3);
        assertThat(sale.amount().getAmount()).isEqualByComparingTo("50000");

        DashboardSummary.RecentActivity receipt = activities.get(2);
        assertThat(receipt.resourceId()).isEqualTo(receiptOperation);
        assertThat(receipt.itemCount()).isEqualTo(2);
        assertThat(receipt.quantity()).isEqualTo(7);
        assertThat(receipt.amount()).isNull();
    }

    private DashboardQueryCriteria criteria() {
        return new DashboardQueryCriteria(
                NOW,
                NOW.toLocalDate().atStartOfDay(),
                NOW.toLocalDate().plusDays(1).atStartOfDay(),
                NOW.toLocalDate().minusDays(6).atStartOfDay(),
                NOW.toLocalDate().minusDays(13).atStartOfDay(),
                NOW.minusDays(31),
                XAF,
                5,
                8);
    }

    private UUID product(String name, String reference, int threshold, boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product
                    (id, name, reference, category_id, minimum_global_threshold,
                     unit_price_amount, unit_price_currency, active)
                VALUES (?, ?, ?, ?, ?, ?, 'XAF', ?)
                """, id, unique(name), unique(reference), categoryId, threshold,
                new BigDecimal("10000"), active);
        return id;
    }

    private void stock(UUID productId, int quantity) {
        jdbc.update("INSERT INTO stock_level (location_id, product_id, quantity) VALUES (?, ?, ?)",
                locationId, productId, quantity);
    }

    private UUID sale(LocalDateTime occurredAt, String total) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sale
                    (id, sold_by, occurred_at, total_amount, total_currency, customer_id)
                VALUES (?, ?, ?, ?, 'XAF', NULL)
                """, id, sellerId, occurredAt, new BigDecimal(total));
        return id;
    }

    private UUID creditSale(UUID customerId, LocalDateTime occurredAt, String total) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sale
                    (id, sold_by, occurred_at, total_amount, total_currency, customer_id)
                VALUES (?, ?, ?, ?, 'XAF', ?)
                """, id, sellerId, occurredAt, new BigDecimal(total), customerId);
        return id;
    }

    private UUID customer(String givenName, String fatherName, String phoneNumber) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customer
                    (id, given_name, father_name, phone_number, email, created_at)
                VALUES (?, ?, ?, ?, NULL, ?)
                """, id, givenName, fatherName, phoneNumber, NOW.minusDays(60));
        return id;
    }

    private void payment(UUID saleId, LocalDateTime receivedAt, String amount) {
        jdbc.update("""
                INSERT INTO payment
                    (id, sale_id, amount, currency, received_at, received_by)
                VALUES (?, ?, ?, 'XAF', ?, ?)
                """, UUID.randomUUID(), saleId, new BigDecimal(amount), receivedAt, sellerId);
    }

    private void movement(UUID productId,
                          String type,
                          UUID sourceLocationId,
                          UUID destinationLocationId,
                          int quantity,
                          LocalDateTime occurredAt,
                          UUID saleId,
                          UUID operationId) {
        jdbc.update("""
                INSERT INTO stock_movement
                    (id, product_id, source_location_id, destination_location_id,
                     movement_type, quantity, performed_by, occurred_at, sale_id, operation_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), productId, sourceLocationId, destinationLocationId,
                type, quantity, sellerId, occurredAt, saleId, operationId);
    }

    private void saleLine(UUID saleId,
                          int lineNumber,
                          UUID productId,
                          int quantity,
                          String unitPrice) {
        BigDecimal price = new BigDecimal(unitPrice);
        jdbc.update("""
                INSERT INTO sale_line
                    (sale_id, line_number, product_id, quantity,
                     unit_price_amount, unit_price_currency,
                     line_total_amount, line_total_currency)
                VALUES (?, ?, ?, ?, ?, 'XAF', ?, 'XAF')
                """, saleId, lineNumber, productId, quantity, price,
                price.multiply(BigDecimal.valueOf(quantity)));
    }

    private static String unique(String value) {
        return value + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
