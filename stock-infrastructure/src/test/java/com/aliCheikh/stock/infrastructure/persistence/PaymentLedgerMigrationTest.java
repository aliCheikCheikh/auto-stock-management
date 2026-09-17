package com.aliCheikh.stock.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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

/** Applies V13 to existing sales to verify conversion of initial amounts into payment entries. */
@Testcontainers
class PaymentLedgerMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_ledger_migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void should_turn_the_existing_down_payment_into_the_first_payment() throws Exception {
        migrateTo("12");

        UUID cashSaleId = UUID.randomUUID();
        UUID creditSaleId = UUID.randomUUID();
        UUID unpaidSaleId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        seedLegacyData(sellerId, customerId, cashSaleId, creditSaleId, unpaidSaleId);

        // Apply V13 to sales with existing initial payments.
        migrateTo(null);

        // Each initial payment retains the sale's seller and timestamp.
        assertThat(collectedFor(cashSaleId)).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(collectedFor(creditSaleId)).isEqualByComparingTo(new BigDecimal("20000.00"));

        // Zero initial amounts do not create payment entries.
        assertThat(paymentCountFor(unpaidSaleId)).isZero();

        // The aggregate amount column is removed in favor of the payment ledger.
        assertThat(columnExists("sale", "amount_paid")).isFalse();
        assertThat(columnExists("sale", "amount_paid_currency")).isFalse();
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

    private void seedLegacyData(UUID sellerId,
                                UUID customerId,
                                UUID cashSaleId,
                                UUID creditSaleId,
                                UUID unpaidSaleId) throws Exception {
        LocalDateTime soldAt = LocalDateTime.now().minusDays(10);

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO app_user (id, username, role)
                    VALUES ('%s', 'vendeur.historique', 'SELLER')
                    """.formatted(sellerId));

            statement.execute("""
                    INSERT INTO customer (id, given_name, phone_number)
                    VALUES ('%s', 'Ahmat', '+23566123456')
                    """.formatted(customerId));

            // Cash sale paid in full.
            statement.execute(insertSale(cashSaleId, sellerId, null, soldAt, "50000.00", "50000.00"));
            // Credit sale with an initial payment.
            statement.execute(insertSale(creditSaleId, sellerId, customerId, soldAt, "75000.00", "20000.00"));
            // Credit sale without an initial payment.
            statement.execute(insertSale(unpaidSaleId, sellerId, customerId, soldAt, "30000.00", "0.00"));
        }
    }

    private static String insertSale(UUID saleId,
                                     UUID sellerId,
                                     UUID customerId,
                                     LocalDateTime soldAt,
                                     String total,
                                     String amountPaid) {
        String customer = customerId == null ? "NULL" : "'" + customerId + "'";
        return """
                INSERT INTO sale (id, sold_by, occurred_at, total_amount, total_currency,
                                  customer_id, amount_paid, amount_paid_currency)
                VALUES ('%s', '%s', '%s', %s, 'XAF', %s, %s, 'XAF')
                """.formatted(saleId, sellerId, soldAt, total, customer, amountPaid);
    }

    private BigDecimal collectedFor(UUID saleId) throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE sale_id = ?")) {
            statement.setObject(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    private int paymentCountFor(UUID saleId) throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM payment WHERE sale_id = ?")) {
            statement.setObject(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private boolean columnExists(String table, String column) throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*) FROM information_schema.columns
                     WHERE table_name = ? AND column_name = ?
                     """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
