package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionDescriptor;
import com.aliCheikh.stock.application.dto.StockReceiptImportIssueCode;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionStatus;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowPreview;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.application.exception.StockReceiptImportExecutionConflictException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.adapter.StockReceiptImportExecutionJpaAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(StockReceiptImportExecutionJpaAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockReceiptImportExecutionLedgerPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_import_execution_test")
            .withUsername("test")
            .withPassword("test");

    @org.springframework.test.context.DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private StockReceiptImportExecutionJpaAdapter ledger;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    private ShopId shopId;
    private UserId userId;
    private ProductId productId;

    @BeforeEach
    void seedReferencedData() {
        shopId = ShopId.generate();
        userId = UserId.generate();
        productId = ProductId.generate();
        UUID categoryId = UUID.randomUUID();
        jdbc.update("INSERT INTO shop(id, name, address) VALUES (?, ?, ?)",
                shopId.getValue(), "Boutique import", "Adresse import");
        jdbc.update("""
                INSERT INTO app_user(
                    id, username, role, email, password_hash, password_temporary, active, display_name
                ) VALUES (?, ?, 'OWNER', ?, ?, false, true, ?)
                """,
                userId.getValue(),
                "owner-" + userId,
                "owner-" + userId + "@example.test",
                "test-password-hash",
                "Responsable import");
        jdbc.update("INSERT INTO category(id, name) VALUES (?, ?)",
                categoryId, "Categorie-" + categoryId);
        jdbc.update("""
                INSERT INTO product(
                    id, name, reference, category_id, minimum_global_threshold,
                    unit_price_amount, unit_price_currency, active
                ) VALUES (?, ?, ?, ?, 0, 1000, 'XAF', true)
                """,
                productId.getValue(), "Produit-" + productId, "REF-" + productId, categoryId);
    }

    @Test
    void shouldCreateExecutionIdempotentlyAndRejectIdentityReuse() {
        UUID importId = UUID.randomUUID();
        StockReceiptImportExecutionDescriptor descriptor = descriptor(importId, "a".repeat(64));

        ledger.ensureExecution(descriptor);
        ledger.ensureExecution(descriptor);

        StockReceiptImportExecutionDescriptor conflicting = new StockReceiptImportExecutionDescriptor(
                importId, "b".repeat(64), descriptor.shopId(), descriptor.userId());
        assertThatThrownBy(() -> ledger.ensureExecution(conflicting))
                .isInstanceOf(StockReceiptImportExecutionConflictException.class);
    }

    @Test
    void shouldRoundTripImportedAndFailedRowsInsideLockedTransactions() {
        UUID importId = UUID.randomUUID();
        ledger.ensureExecution(descriptor(importId, "c".repeat(64)));
        StockReceiptImportRowExecutionResult imported = StockReceiptImportRowExecutionResult.imported(
                2, StockReceiptImportRowAction.CREATE_PRODUCT, "REF-1", "Filtre", productId, 10);
        StockReceiptImportRowExecutionResult failed = StockReceiptImportRowExecutionResult.failed(
                readyExistingRow(3, "REF-2", "Plaquette"));

        transactions.executeWithoutResult(ignored -> {
            ledger.lockExecution(importId);
            ledger.saveRowResult(importId, imported);
            ledger.saveRowResult(importId, failed);
        });

        Optional<StockReceiptImportRowExecutionResult> first =
                transactions.execute(ignored -> ledger.findRowResult(importId, 2));
        Optional<StockReceiptImportRowExecutionResult> second =
                transactions.execute(ignored -> ledger.findRowResult(importId, 3));

        assertThat(first).contains(imported);
        assertThat(second).hasValueSatisfying(result -> {
            assertThat(result.status()).isEqualTo(StockReceiptImportRowExecutionStatus.FAILED);
            assertThat(result.issues()).singleElement()
                    .extracting(issue -> issue.code())
                    .isEqualTo(StockReceiptImportIssueCode.EXECUTION_FAILED);
        });
    }

    @Test
    void shouldRequireAnActiveTransactionToLockOrSaveRows() {
        UUID importId = UUID.randomUUID();
        ledger.ensureExecution(descriptor(importId, "d".repeat(64)));
        StockReceiptImportRowExecutionResult imported = StockReceiptImportRowExecutionResult.imported(
                2, StockReceiptImportRowAction.RECEIVE_EXISTING,
                "REF-1", "Filtre", productId, 4);

        assertThatThrownBy(() -> ledger.lockExecution(importId))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        assertThatThrownBy(() -> ledger.saveRowResult(importId, imported))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    private StockReceiptImportExecutionDescriptor descriptor(UUID importId, String fingerprint) {
        return new StockReceiptImportExecutionDescriptor(
                importId, fingerprint, shopId, userId);
    }

    private static StockReceiptImportRowPreview readyExistingRow(
            int lineNumber,
            String reference,
            String name
    ) {
        return new StockReceiptImportRowPreview(
                lineNumber,
                StockReceiptImportRowAction.RECEIVE_EXISTING,
                reference,
                name,
                "",
                null,
                null,
                null,
                ProductId.generate(),
                List.of(new TargetLocation(LocationId.generate(), 1)),
                List.of()
        );
    }
}
