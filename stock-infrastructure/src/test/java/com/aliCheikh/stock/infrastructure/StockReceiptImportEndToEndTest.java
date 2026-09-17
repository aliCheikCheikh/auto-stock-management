package com.aliCheikh.stock.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CSV import and idempotency through the full application stack against PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StockReceiptImportEndToEndTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_import_e2e")
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

    private UUID userId;
    private UUID shopId;
    private UUID shopFloorId;
    private UUID backstockId;
    private UUID existingProductId;
    private String categoryName;
    private String existingReference;
    private String newReference;
    private byte[] csv;

    @BeforeEach
    void seedImportContext() {
        userId = UUID.randomUUID();
        shopId = UUID.randomUUID();
        shopFloorId = UUID.randomUUID();
        backstockId = UUID.randomUUID();
        existingProductId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        categoryName = "Import moteur " + categoryId;
        existingReference = "IMPORT-EXISTING-" + existingProductId;
        newReference = "IMPORT-NEW-" + UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO app_user (id, username, display_name, role, password_temporary, active)
                VALUES (?, ?, 'Gestionnaire import', 'SELLER', false, true)
                """, userId, "import." + userId);
        jdbcTemplate.update("INSERT INTO category (id, name) VALUES (?, ?)", categoryId, categoryName);
        jdbcTemplate.update("INSERT INTO shop (id, name, address) VALUES (?, 'Boutique import', 'N''Djamena')",
                shopId);
        jdbcTemplate.update("""
                INSERT INTO storage_location (id, shop_id, location_type, label, low_stock_indicator, version)
                VALUES (?, ?, 'SHOP_FLOOR', 'Surface de vente', 0, 0)
                """, shopFloorId, shopId);
        jdbcTemplate.update("""
                INSERT INTO storage_location (id, shop_id, location_type, label, low_stock_indicator, version)
                VALUES (?, ?, 'BACKSTOCK', 'Réserve', 0, 0)
                """, backstockId, shopId);
        jdbcTemplate.update("""
                INSERT INTO product (id, name, reference, category_id, minimum_global_threshold,
                                     unit_price_amount, unit_price_currency, active)
                VALUES (?, ?, ?, ?, 1, 9000, 'XAF', true)
                """, existingProductId, "Produit existant " + existingProductId, existingReference, categoryId);

        csv = ("reference;nom_produit;categorie;prix_unitaire_xaf;seuil_alerte;"
                + "quantite_surface;quantite_reserve\n"
                + newReference + ";Filtre importé;" + categoryName + ";12500;5;8;2\n"
                + existingReference + ";;;;;3;4\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void imports_new_and_existing_products_then_replays_without_doubling_stock() throws Exception {
        mockMvc.perform(multipart("/api/v1/stock-receipts/import-preview")
                        .file(csvFile())
                        .param("shopId", shopId.toString())
                        .with(authentication(seller())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalRows").value(2))
                .andExpect(jsonPath("$.summary.productsToCreate").value(1))
                .andExpect(jsonPath("$.summary.existingProductsToReceive").value(1))
                .andExpect(jsonPath("$.summary.invalidRows").value(0))
                .andExpect(jsonPath("$.rows[0].action").value("CREATE_PRODUCT"))
                .andExpect(jsonPath("$.rows[1].action").value("RECEIVE_EXISTING"));

        UUID importId = UUID.randomUUID();
        performImport(importId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.importedRows").value(2))
                .andExpect(jsonPath("$.summary.productsCreated").value(1))
                .andExpect(jsonPath("$.summary.existingProductsReceived").value(1))
                .andExpect(jsonPath("$.summary.totalQuantityReceived").value(17))
                .andExpect(jsonPath("$.rows[0].status").value("IMPORTED"))
                .andExpect(jsonPath("$.rows[1].status").value("IMPORTED"));

        UUID newProductId = jdbcTemplate.queryForObject(
                "SELECT id FROM product WHERE reference = ?", UUID.class, newReference);
        assertStock(newProductId, shopFloorId, 8);
        assertStock(newProductId, backstockId, 2);
        assertStock(existingProductId, shopFloorId, 3);
        assertStock(existingProductId, backstockId, 4);
        assertThat(count("SELECT COUNT(*) FROM stock_movement")).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM stock_receipt_import_row_result")).isEqualTo(2);

        performImport(importId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.importedRows").value(2))
                .andExpect(jsonPath("$.summary.totalQuantityReceived").value(17));

        assertStock(newProductId, shopFloorId, 8);
        assertStock(newProductId, backstockId, 2);
        assertStock(existingProductId, shopFloorId, 3);
        assertStock(existingProductId, backstockId, 4);
        assertThat(count("SELECT COUNT(*) FROM stock_movement")).isEqualTo(4);
        Integer matchingProducts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE reference = ?",
                Integer.class,
                newReference
        );
        assertThat(matchingProducts).isEqualTo(1);
    }

    private ResultActions performImport(UUID importId) throws Exception {
        return mockMvc.perform(multipart("/api/v1/stock-receipts/import-executions")
                .file(csvFile())
                .param("shopId", shopId.toString())
                .param("importId", importId.toString())
                .param("selectedLineNumbers", "2", "3")
                .with(authentication(seller())));
    }

    private MockMultipartFile csvFile() {
        return new MockMultipartFile("file", "produits.csv", "text/csv", csv);
    }

    private UsernamePasswordAuthenticationToken seller() {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
    }

    private void assertStock(UUID productId, UUID locationId, int expectedQuantity) {
        Integer quantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM stock_level WHERE product_id = ? AND location_id = ?",
                Integer.class,
                productId,
                locationId
        );
        assertThat(quantity).isEqualTo(expectedQuantity);
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
