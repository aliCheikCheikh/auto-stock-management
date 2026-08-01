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

/**
 * Parcours complet d'une vente à crédit, de la requête HTTP jusqu'à la base.
 *
 * <p>Chaque couche est déjà couverte séparément ; ce test vérifie ce qu'aucune d'elles ne peut
 * prouver seule : que l'assemblage tient. Il traverse le contrôleur, le use case, le domaine, les
 * mappers et une vraie PostgreSQL.</p>
 */
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
        // Ordre imposé par les clés étrangères : les mouvements référencent la vente.
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

        // password_temporary vaut TRUE par défaut : TemporaryPasswordFilter refuserait alors toute
        // requête hors authentification. Ce vendeur est un compte déjà activé.
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

        // 2 × 25 000 = 50 000, dont 20 000 versés au comptoir.
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

        // La créance apparaît telle quelle dans la vue du patron.
        mockMvc.perform(get("/api/v1/debts").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$[0].customerGivenName").value("Ahmat"))
                .andExpect(jsonPath("$[0].amountDue.amount").value("30000.00"));

        // Premier remboursement : la dette diminue, elle ne disparaît pas.
        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 12000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountDue.amount").value("18000.00"))
                .andExpect(jsonPath("$.settled").value(false));

        // Second remboursement : la vente est soldée et sort de la liste des créances.
        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 18000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountDue.amount").value("0.00"))
                .andExpect(jsonPath("$.settled").value(true));

        mockMvc.perform(get("/api/v1/debts").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Les trois encaissements sont bien tracés en base, aucun n'a été écrasé.
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

        // Aucune vente n'a été enregistrée : le refus est total, pas partiel.
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

    /**
     * Le détail d'une créance vit sous {@code /debts} précisément pour hériter de cette règle sans
     * ligne de configuration supplémentaire. Tant que rien ne le vérifie, l'argument n'est qu'une
     * intention : un jour où {@code /debts/**} redeviendrait {@code /debts}, la route exposerait le
     * nom et le téléphone du client à tout le personnel, en silence.
     */
    @Test
    void a_seller_cannot_read_the_detail_of_a_debt() throws Exception {
        mockMvc.perform(get("/api/v1/debts/{saleId}", UUID.randomUUID())
                        .with(authentication(seller())))
                .andExpect(status().isForbidden());
    }

    /**
     * Le parcours que réclamait le patron : depuis la liste des créances, ouvrir une vente et voir
     * quels produits ont été vendus, par qui, quand, ce qui a été versé et ce qui reste.
     */
    @Test
    void the_owner_can_open_a_debt_and_see_what_was_sold_and_what_remains() throws Exception {
        UUID customerId = createCustomer();

        // 3 × 25 000 = 75 000, dont 25 000 versés au comptoir : il reste 50 000.
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
                // « Quel produit, en quelle quantité ? » — nommé, pas un identifiant.
                .andExpect(jsonPath("$.lines[0].productName").value("Plaquettes " + productId))
                .andExpect(jsonPath("$.lines[0].quantity").value(3))
                .andExpect(jsonPath("$.lines[0].lineTotal.amount").value("75000.00"))
                // « Vendu par qui, à qui, quand ? »
                .andExpect(jsonPath("$.sellerName").value("Vendeur"))
                .andExpect(jsonPath("$.customerGivenName").value("Ahmat"))
                .andExpect(jsonPath("$.occurredAt").exists())
                // « Combien versé, combien reste-t-il ? »
                .andExpect(jsonPath("$.amountPaid.amount").value("25000.00"))
                .andExpect(jsonPath("$.amountDue.amount").value("50000.00"))
                .andExpect(jsonPath("$.settled").value(false))
                // L'acompte du comptoir ouvre l'échéancier, avec le nom de qui l'a reçu.
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].amount.amount").value("25000.00"))
                .andExpect(jsonPath("$.payments[0].receivedByName").value("Vendeur"));

        // Et l'historique des mouvements annonce la même chose, sans quitter l'écran.
        mockMvc.perform(get("/api/v1/stock-movements").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].saleAmountDue.amount").value("50000.00"));
    }

    /** Une créance soldée reste consultable : c'est la preuve du règlement, pas un déchet. */
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

        // Rien n'a été versé au comptoir : la créance porte le total, et le ledger est vide.
        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId).with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountDue.amount").value("25000.00"))
                .andExpect(jsonPath("$.payments").isEmpty());

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .with(authentication(seller()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 25000}"))
                .andExpect(status().isCreated());

        // La vente a quitté la liste des créances, mais son détail reste opposable au client.
        mockMvc.perform(get("/api/v1/debts").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId).with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(true))
                .andExpect(jsonPath("$.amountDue.amount").value("0.00"))
                .andExpect(jsonPath("$.payments.length()").value(1));
    }

    /** Une vente au comptant n'est pas une créance : elle n'a rien à faire sur cet écran. */
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

        // Mais l'historique annonce bien qu'elle a été réglée : zéro, et non « pas d'information ».
        mockMvc.perform(get("/api/v1/stock-movements").with(authentication(owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].saleAmountDue.amount").value("0.00"));
    }
}
