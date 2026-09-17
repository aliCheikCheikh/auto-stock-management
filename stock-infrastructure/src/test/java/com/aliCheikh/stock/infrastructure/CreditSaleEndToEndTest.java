package com.aliCheikh.stock.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Credit sale flow from HTTP through the application and domain layers to PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CreditSaleEndToEndTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_e2e")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID sellerId;
    private UUID shopId;
    private UUID productId;

    @BeforeEach
    void seedShop() {
        // Delete movements before sales to respect foreign keys.
        jdbcTemplate.update("DELETE FROM stock_movement");
        jdbcTemplate.update("DELETE FROM payment");
        jdbcTemplate.update("DELETE FROM sale_line");
        jdbcTemplate.update("DELETE FROM sale");
        jdbcTemplate.update("DELETE FROM stock_level");
        jdbcTemplate.update("DELETE FROM customer");

        sellerId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        // Use an onboarded seller; the default temporary-password flag would block business
        // requests.
        jdbcTemplate.update("""
                INSERT INTO app_user (id, username, display_name, role, password_temporary, active)
                VALUES (?, ?, 'Vendeur', 'SELLER', false, true)
                """, sellerId, "vendeur." + sellerId);
        jdbcTemplate.update("INSERT INTO category (id, name) VALUES (?, ?)", categoryId, "Freinage " + categoryId);
        jdbcTemplate.update("""
                INSERT INTO product (id, name, reference, category_id, minimum_global_threshold,
                                     unit_price_amount, unit_price_currency, active)
                VALUES (?, ?, ?, ?, 1, 25000, 'XAF', true)
                """, productId, "Plaquettes " + productId, "REF-" + productId, categoryId);
        jdbcTemplate.update("INSERT INTO shop (id, name, address) VALUES (?, 'Boutique', 'N''Djamena')", shopId);
        jdbcTemplate.update("""
                INSERT INTO storage_location (id, shop_id, location_type, label, low_stock_indicator, version)
                VALUES (?, ?, 'SHOP_FLOOR', 'Surface de vente', 2, 0)
                """, locationId, shopId);
        jdbcTemplate.update("INSERT INTO stock_level (location_id, product_id, quantity) VALUES (?, ?, 10)",
                locationId, productId);
    }

    private UsernamePasswordAuthenticationToken seller() {
        return new UsernamePasswordAuthenticationToken(
                sellerId, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
    }

    private UsernamePasswordAuthenticationToken owner() {
        return new UsernamePasswordAuthenticationToken(
                sellerId, null, List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
    }

    private UUID createCustomer() throws Exception {
        String body = mockMvc.perform(post("/api/v1/customers")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"givenName": "Ahmat", "fatherName": "Youssouf", "phoneNumber": "66 12 34 56"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(body).get("customerId").asText());
    }

    @Test
    void a_credit_sale_can_be_recorded_then_settled_by_successive_payments() throws Exception {
        UUID customerId = createCustomer();

        // Two items at 25,000 total 50,000, with 20,000 initially paid.
        String saleBody = mockMvc.perform(post("/api/v1/sales")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [{"productId": "%s", "quantity": 2}],
                                  "customerId": "%s",
                                  "amountPaid": 20000
                                }
                                """.formatted(shopId, productId, customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountDue.amount").value("30000.00"))
                .andReturn().getResponse().getContentAsString();

        UUID saleId = UUID.fromString(objectMapper.readTree(saleBody).get("saleId").asText());

        // The owner sees the outstanding debt.
        mockMvc.perform(get("/api/v1/debts").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].customerGivenName").value("Ahmat"))
                .andExpect(jsonPath("$.content[0].amountDue.amount").value("30000.00"))
                .andExpect(jsonPath("$.content[0].settled").value(false));

        // The first repayment reduces the debt without settling it.
        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 12000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountDue.amount").value("18000.00"))
                .andExpect(jsonPath("$.settled").value(false));

        // The second repayment settles the sale.
        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 18000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountDue.amount").value("0.00"))
                .andExpect(jsonPath("$.settled").value(true));

        // The sale leaves the outstanding list.
        mockMvc.perform(get("/api/v1/debts").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        // It remains accessible in settlement history.
        mockMvc.perform(get("/api/v1/debts")
                        .param("status", "SETTLED")
                        .with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].settled").value(true))
                .andExpect(jsonPath("$.content[0].amountDue.amount").value("0.00"))
                .andExpect(jsonPath("$.content[0].settledAt").exists())
                // Same-day settlement gives a debt age of zero.
                .andExpect(jsonPath("$.content[0].daysOutstanding").value(0))
                .andExpect(jsonPath("$.content[0].overdue").value(false));

        // All three payments remain in the ledger.
        Integer payments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE sale_id = ?", Integer.class, saleId);
        assertThat(payments).isEqualTo(3);
    }

    @Test
    void a_credit_sale_without_customer_is_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [{"productId": "%s", "quantity": 1}],
                                  "amountPaid": 5000
                                }
                                """.formatted(shopId, productId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CREDIT_SALE_REQUIRES_CUSTOMER"));

        // The rejected request must not create a sale.
        Integer sales = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sale", Integer.class);
        assertThat(sales).isZero();
    }

    @Test
    void a_payment_larger_than_the_balance_is_rejected() throws Exception {
        UUID customerId = createCustomer();

        String saleBody = mockMvc.perform(post("/api/v1/sales")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [{"productId": "%s", "quantity": 1}],
                                  "customerId": "%s",
                                  "amountPaid": 0
                                }
                                """.formatted(shopId, productId, customerId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID saleId = UUID.fromString(objectMapper.readTree(saleBody).get("saleId").asText());

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 99000}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_EXCEEDS_AMOUNT_DUE"));

        Integer payments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE sale_id = ?", Integer.class, saleId);
        assertThat(payments).isZero();
    }

    @Test
    void a_seller_cannot_read_the_debts_list() throws Exception {
        mockMvc.perform(get("/api/v1/debts").with(authentication(seller())))
                .andExpect(status().isForbidden());
    }

    /** Debt history requires the same access rights as the outstanding list. */
    @Test
    void a_seller_cannot_read_the_settled_debts_history() throws Exception {
        mockMvc.perform(get("/api/v1/debts")
                        .param("status", "SETTLED")
                        .with(authentication(seller())))
                .andExpect(status().isForbidden());
    }

    /**
     * Customer history filters outstanding and settled sales and counts sales rather than
     * payments.
     */
    @Test
    void the_owner_can_read_what_a_customer_owes_and_what_he_already_repaid() throws Exception {
        UUID customerId = createCustomer();

        UUID stillOwed = createCreditSale(customerId, 2, 0);
        UUID repaid = createCreditSale(customerId, 1, 0);

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", repaid)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 25000}"))
                .andExpect(status().isCreated());

        // Customer history defaults to outstanding debts.
        mockMvc.perform(get("/api/v1/customers/{customerId}/debts", customerId)
                        .with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].saleId").value(stillOwed.toString()))
                .andExpect(jsonPath("$.content[0].amountDue.amount").value("50000.00"));

        // Settled debts for the same customer.
        mockMvc.perform(get("/api/v1/customers/{customerId}/debts", customerId)
                        .param("status", "SETTLED")
                        .with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].saleId").value(repaid.toString()))
                .andExpect(jsonPath("$.content[0].settledAt").exists());

        // Both statuses, with a count of sales.
        mockMvc.perform(get("/api/v1/customers/{customerId}/debts", customerId)
                        .param("status", "ALL")
                        .with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        // A single-item page retains pagination metadata.
        mockMvc.perform(get("/api/v1/customers/{customerId}/debts", customerId)
                        .param("status", "ALL")
                        .param("size", "1")
                        .with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    private UUID createCreditSale(UUID customerId, int quantity, int amountPaid) throws Exception {
        String body = mockMvc.perform(post("/api/v1/sales")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [{"productId": "%s", "quantity": %d}],
                                  "customerId": "%s",
                                  "amountPaid": %d
                                }
                                """.formatted(shopId, productId, quantity, customerId, amountPaid)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(body).get("saleId").asText());
    }

    /** Debt details must inherit the owner-only /debts/** authorization rule. */
    @Test
    void a_seller_cannot_read_the_detail_of_a_debt() throws Exception {
        mockMvc.perform(get("/api/v1/debts/{saleId}", UUID.randomUUID())
                        .with(authentication(seller())))
                .andExpect(status().isForbidden());
    }

    /** Credit sale details include products, seller, customer, payments and balance. */
    @Test
    void the_owner_can_open_a_debt_and_see_what_was_sold_and_what_remains() throws Exception {
        UUID customerId = createCustomer();

        // Three items at 25,000 total 75,000, with 25,000 paid and 50,000 due.
        String saleBody = mockMvc.perform(post("/api/v1/sales")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [{"productId": "%s", "quantity": 3}],
                                  "customerId": "%s",
                                  "amountPaid": 25000
                                }
                                """.formatted(shopId, productId, customerId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID saleId = UUID.fromString(objectMapper.readTree(saleBody).get("saleId").asText());

        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId).with(authentication(owner())))
                .andExpect(status().isOk())
                // Product names and quantities.
                .andExpect(jsonPath("$.lines[0].productName").value("Plaquettes " + productId))
                .andExpect(jsonPath("$.lines[0].quantity").value(3))
                .andExpect(jsonPath("$.lines[0].lineTotal.amount").value("75000.00"))
                // Seller, customer and sale timestamp.
                .andExpect(jsonPath("$.sellerName").value("Vendeur"))
                .andExpect(jsonPath("$.customerGivenName").value("Ahmat"))
                .andExpect(jsonPath("$.occurredAt").exists())
                // Payments and remaining balance.
                .andExpect(jsonPath("$.amountPaid.amount").value("25000.00"))
                .andExpect(jsonPath("$.amountDue.amount").value("50000.00"))
                .andExpect(jsonPath("$.settled").value(false))
                // The initial payment includes its receiver's name.
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].amount.amount").value("25000.00"))
                .andExpect(jsonPath("$.payments[0].receivedByName").value("Vendeur"));

        // Movement history exposes the same settlement state.
        mockMvc.perform(get("/api/v1/stock-movements").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].saleAmountDue.amount").value("50000.00"));
    }

    /** Settled debt details remain accessible as payment history. */
    @Test
    void the_detail_of_a_debt_survives_its_settlement() throws Exception {
        UUID customerId = createCustomer();

        String saleBody = mockMvc.perform(post("/api/v1/sales")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [{"productId": "%s", "quantity": 1}],
                                  "customerId": "%s",
                                  "amountPaid": 0
                                }
                                """.formatted(shopId, productId, customerId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID saleId = UUID.fromString(objectMapper.readTree(saleBody).get("saleId").asText());

        // No initial payment: the ledger is empty and the full total is due.
        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId).with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountDue.amount").value("25000.00"))
                .andExpect(jsonPath("$.payments").isEmpty());

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 25000}"))
                .andExpect(status().isCreated());

        // Settled sale details remain accessible after leaving the outstanding list.
        mockMvc.perform(get("/api/v1/debts").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId).with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(true))
                .andExpect(jsonPath("$.amountDue.amount").value("0.00"))
                .andExpect(jsonPath("$.payments.length()").value(1));
    }

    /** A cash sale without a customer is absent from the debt view. */
    @Test
    void a_cash_sale_has_no_debt_detail() throws Exception {
        String saleBody = mockMvc.perform(post("/api/v1/sales")
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [{"productId": "%s", "quantity": 1}]
                                }
                                """.formatted(shopId, productId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID saleId = UUID.fromString(objectMapper.readTree(saleBody).get("saleId").asText());

        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId).with(authentication(owner())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SALE_NOT_FOUND"));

        // History reports a zero balance rather than an unknown balance.
        mockMvc.perform(get("/api/v1/stock-movements").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].saleAmountDue.amount").value("0.00"));
    }
}
