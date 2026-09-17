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
 * Applies V12 to a populated V11 database to verify backfilling existing sales before adding NOT
 * NULL constraints.
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

        // Pin the target to V12: V13 removes amount_paid, while this test verifies its backfill.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target(org.flywaydb.core.api.MigrationVersion.fromVersion("12"))
                .load()
                .migrate();

        // Legacy sales are treated as fully paid without a customer.
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
