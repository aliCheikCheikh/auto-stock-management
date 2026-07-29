package com.aliCheikh.stock.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie la migration V12 contre une base <b>déjà peuplée</b>.
 *
 * <p>Un test qui part d'une base vide ne prouverait rien : l'étape de remplissage ne toucherait
 * aucune ligne, et le passage en NOT NULL réussirait quelle que soit sa correction. On rejoue donc
 * les migrations jusqu'à V11, on insère une vente « ancienne », puis on applique V12.</p>
 */
@Testcontainers
class CreditSaleMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void should_backfill_existing_sales_as_fully_paid() throws Exception {
        Flyway upToV11 = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(org.flywaydb.core.api.MigrationVersion.fromVersion("11"))
                .cleanDisabled(false)
                .load();
        upToV11.clean();
        upToV11.migrate();

        UUID saleId = UUID.randomUUID();
        insertLegacySale(saleId);

        // WHEN : on applique V12 sur une base qui contient déjà une vente.
        // La cible est épinglée : V13 supprime amount_paid, ce test porte sur son remplissage.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(org.flywaydb.core.api.MigrationVersion.fromVersion("12"))
                .load()
                .migrate();

        // THEN : la vente historique est considérée payée comptant, sans client
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT amount_paid, amount_paid_currency, customer_id FROM sale WHERE id = ?")) {
            statement.setObject(1, saleId);

            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBigDecimal("amount_paid"))
                        .isEqualByComparingTo(new BigDecimal("50000.00"));
                assertThat(resultSet.getString("amount_paid_currency")).isEqualTo("XAF");
                assertThat(resultSet.getObject("customer_id")).isNull();
            }
        }
    }

    private void insertLegacySale(UUID saleId) throws Exception {
        UUID sellerId = UUID.randomUUID();

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_user (id, username, role)
                    VALUES ('%s', 'vendeur.historique', 'SELLER')
                    """.formatted(sellerId));

            statement.execute("""
                    INSERT INTO sale (id, sold_by, occurred_at, total_amount, total_currency)
                    VALUES ('%s', '%s', '%s', 50000.00, 'XAF')
                    """.formatted(saleId, sellerId, LocalDateTime.now()));
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
