package com.aliCheikh.stock.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie la migration V16 contre une base <b>déjà peuplée</b> de mouvements.
 *
 * <p>Le point sensible n'est pas la colonne mais la reprise : les sorties d'une même vente doivent
 * rester groupées, tandis que les réceptions et transferts antérieurs — pour lesquels aucun
 * regroupement n'est reconstituable — doivent devenir chacun leur propre opération plutôt que d'être
 * fusionnés à tort.</p>
 */
@Testcontainers
class MovementOperationMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_operation_migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void should_group_past_sale_movements_and_isolate_the_others() throws Exception {
        migrateTo("15");

        UUID sellerId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        UUID firstExitId = UUID.randomUUID();
        UUID secondExitId = UUID.randomUUID();
        UUID firstEntryId = UUID.randomUUID();
        UUID secondEntryId = UUID.randomUUID();

        seedLegacyMovements(sellerId, saleId, firstExitId, secondExitId, firstEntryId, secondEntryId);

        // WHEN : V16 s'applique sur une base contenant déjà des mouvements
        migrateTo(null);

        // Deux sorties de la même vente : un seul groupe, hérité de la vente.
        assertThat(operationOf(firstExitId)).isEqualTo(operationOf(secondExitId));
        assertThat(operationOf(firstExitId)).isEqualTo(saleId);

        // Deux entrées anciennes : aucun lien reconstituable, donc deux opérations distinctes.
        // Les fusionner aurait inventé une réception qui n'a jamais existé.
        assertThat(operationOf(firstEntryId)).isNotEqualTo(operationOf(secondEntryId));

        // Aucune ligne ne reste sans opération.
        assertThat(countWithoutOperation()).isZero();
    }

    private void migrateTo(String version) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false);

        if (version != null) {
            configuration = configuration.target(MigrationVersion.fromVersion(version));
            Flyway flyway = configuration.load();
            flyway.clean();
            flyway.migrate();
            return;
        }

        configuration.load().migrate();
    }

    private void seedLegacyMovements(UUID sellerId,
                                     UUID saleId,
                                     UUID firstExitId,
                                     UUID secondExitId,
                                     UUID firstEntryId,
                                     UUID secondEntryId) throws Exception {
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now().minusDays(5);

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_user (id, username, display_name, role)
                    VALUES ('%s', 'vendeur.historique', 'Ahmat', 'SELLER')
                    """.formatted(sellerId));
            // V7 sème déjà les familles par défaut, dont « Freinage » : le nom doit être
            // propre au test, sinon l'insertion viole l'unicité posée par V14.
            statement.execute("INSERT INTO category (id, name) VALUES ('%s', 'Test-%s')"
                    .formatted(categoryId, categoryId));
            statement.execute("""
                    INSERT INTO product (id, name, reference, category_id, minimum_global_threshold,
                                         unit_price_amount, unit_price_currency, active)
                    VALUES ('%s', 'Plaquettes', 'REF-1', '%s', 1, 25000, 'XAF', true)
                    """.formatted(productId, categoryId));
            statement.execute("INSERT INTO shop (id, name, address) VALUES ('%s', 'Boutique', 'N''Djamena')".formatted(shopId));
            statement.execute("""
                    INSERT INTO storage_location (id, shop_id, location_type, label, low_stock_indicator, version)
                    VALUES ('%s', '%s', 'SHOP_FLOOR', 'Surface', 2, 0)
                    """.formatted(locationId, shopId));
            statement.execute("""
                    INSERT INTO sale (id, sold_by, occurred_at, total_amount, total_currency)
                    VALUES ('%s', '%s', '%s', 50000.00, 'XAF')
                    """.formatted(saleId, sellerId, occurredAt));

            statement.execute(insertExit(firstExitId, productId, locationId, sellerId, saleId, occurredAt));
            statement.execute(insertExit(secondExitId, productId, locationId, sellerId, saleId, occurredAt));
            statement.execute(insertEntry(firstEntryId, productId, locationId, sellerId, occurredAt));
            statement.execute(insertEntry(secondEntryId, productId, locationId, sellerId, occurredAt));
        }
    }

    private static String insertExit(UUID id, UUID productId, UUID locationId, UUID sellerId,
                                     UUID saleId, LocalDateTime occurredAt) {
        return """
                INSERT INTO stock_movement (id, product_id, source_location_id, movement_type,
                                            quantity, performed_by, occurred_at, sale_id)
                VALUES ('%s', '%s', '%s', 'EXIT', 1, '%s', '%s', '%s')
                """.formatted(id, productId, locationId, sellerId, occurredAt, saleId);
    }

    private static String insertEntry(UUID id, UUID productId, UUID locationId, UUID sellerId,
                                      LocalDateTime occurredAt) {
        return """
                INSERT INTO stock_movement (id, product_id, destination_location_id, movement_type,
                                            quantity, performed_by, occurred_at)
                VALUES ('%s', '%s', '%s', 'ENTRY', 10, '%s', '%s')
                """.formatted(id, productId, locationId, sellerId, occurredAt);
    }

    private UUID operationOf(UUID movementId) throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT operation_id FROM stock_movement WHERE id = ?")) {
            statement.setObject(1, movementId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getObject(1, UUID.class);
            }
        }
    }

    private int countWithoutOperation() throws Exception {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM stock_movement WHERE operation_id IS NULL")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
