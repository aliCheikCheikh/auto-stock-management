package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.ReceiveStockResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionCommand;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionDescriptor;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionReport;
import com.aliCheikh.stock.application.dto.StockReceiptImportIssue;
import com.aliCheikh.stock.application.dto.StockReceiptImportIssueCode;
import com.aliCheikh.stock.application.dto.StockReceiptImportPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionStatus;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportSummary;
import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.application.exception.InvalidStockReceiptImportSelectionException;
import com.aliCheikh.stock.application.port.StockReceiptImportExecutionLedger;
import com.aliCheikh.stock.application.port.StockReceiptImportFailureReporter;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.service.StockReceiptImportExecutionFingerprint;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecuteStockReceiptImportUseCaseTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    private PrepareStockReceiptImportUseCase prepareUseCase;
    private ReceiveStockUseCase receiveStockUseCase;
    private StockReceiptImportExecutionLedger ledger;
    private StockReceiptImportFailureReporter failureReporter;
    private ExecuteStockReceiptImportUseCase useCase;
    private UUID importId;
    private ShopId shopId;
    private UserId userId;
    private StockReceiptImportFile file;

    @BeforeEach
    void setUp() {
        prepareUseCase = mock(PrepareStockReceiptImportUseCase.class);
        receiveStockUseCase = mock(ReceiveStockUseCase.class);
        ledger = mock(StockReceiptImportExecutionLedger.class);
        failureReporter = mock(StockReceiptImportFailureReporter.class);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override
            public <T> T execute(Supplier<T> work) {
                return work.get();
            }
        };
        useCase = new ExecuteStockReceiptImportUseCase(
                prepareUseCase,
                receiveStockUseCase,
                ledger,
                failureReporter,
                transactionRunner,
                new StockReceiptImportExecutionFingerprint()
        );
        importId = UUID.randomUUID();
        shopId = ShopId.generate();
        userId = UserId.generate();
        file = new StockReceiptImportFile(
                "produits.csv",
                "reference;nom_produit\nREF-1;Filtre".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    void shouldImportSelectedReadyRowsAndReportEveryFileRow() {
        StockReceiptImportRowPreview creation = creationRow(2, "REF-1", "Filtre à huile", 7, 3);
        StockReceiptImportRowPreview existing = existingRow(3, "REF-2", "Plaquette", 4);
        StockReceiptImportRowPreview invalid = invalidRow(4, "REF-3");
        StockReceiptImportRowPreview ignored = existingRow(5, "REF-4", "Courroie", 2);
        givenPreview(creation, existing, invalid, ignored);
        when(ledger.findRowResult(eq(importId), anyInt())).thenReturn(Optional.empty());

        ProductId createdProductId = ProductId.generate();
        ProductId existingProductId = existing.productId();
        when(receiveStockUseCase.execute(any(ReceiveStockCommand.class)))
                .thenReturn(receipt(createdProductId, 10), receipt(existingProductId, 4));

        StockReceiptImportExecutionReport report = useCase.execute(command(Set.of(2, 3)));

        assertThat(report.rows()).extracting(StockReceiptImportRowExecutionResult::status)
                .containsExactly(
                        StockReceiptImportRowExecutionStatus.IMPORTED,
                        StockReceiptImportRowExecutionStatus.IMPORTED,
                        StockReceiptImportRowExecutionStatus.IGNORED_INVALID,
                        StockReceiptImportRowExecutionStatus.IGNORED_BY_USER
                );
        assertThat(report.summary().totalRows()).isEqualTo(4);
        assertThat(report.summary().selectedRows()).isEqualTo(2);
        assertThat(report.summary().importedRows()).isEqualTo(2);
        assertThat(report.summary().productsCreated()).isEqualTo(1);
        assertThat(report.summary().existingProductsReceived()).isEqualTo(1);
        assertThat(report.summary().ignoredInvalidRows()).isEqualTo(1);
        assertThat(report.summary().ignoredByUserRows()).isEqualTo(1);
        assertThat(report.summary().failedRows()).isZero();
        assertThat(report.summary().totalQuantityReceived()).isEqualTo(14);

        ArgumentCaptor<ReceiveStockCommand> commands = ArgumentCaptor.forClass(ReceiveStockCommand.class);
        verify(receiveStockUseCase, org.mockito.Mockito.times(2)).execute(commands.capture());
        assertThat(commands.getAllValues().get(0).newProductInfo()).isNotNull();
        assertThat(commands.getAllValues().get(0).newProductInfo().reference()).isEqualTo("REF-1");
        assertThat(commands.getAllValues().get(1).newProductInfo()).isNull();
        verify(ledger).ensureExecution(any(StockReceiptImportExecutionDescriptor.class));
        verify(ledger, org.mockito.Mockito.times(2)).lockExecution(importId);
    }

    @Test
    void shouldReuseStoredRowResultWithoutReceivingStockAgain() {
        StockReceiptImportRowPreview row = existingRow(2, "REF-2", "Plaquette", 4);
        givenPreview(row);
        StockReceiptImportRowExecutionResult stored = StockReceiptImportRowExecutionResult.imported(
                2, StockReceiptImportRowAction.RECEIVE_EXISTING, "REF-2", "Plaquette",
                row.productId(), 4);
        when(ledger.findRowResult(importId, 2)).thenReturn(Optional.of(stored));

        StockReceiptImportExecutionReport report = useCase.execute(command(Set.of(2)));

        assertThat(report.rows()).containsExactly(stored);
        verify(receiveStockUseCase, never()).execute(any());
        verify(ledger, never()).saveRowResult(any(), any());
    }

    @Test
    void shouldPersistAndReportAControlledFailureForOneRow() {
        StockReceiptImportRowPreview row = existingRow(2, "REF-2", "Plaquette", 4);
        givenPreview(row);
        when(ledger.findRowResult(importId, 2)).thenReturn(Optional.empty());
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(receiveStockUseCase.execute(any())).thenThrow(failure);

        StockReceiptImportExecutionReport report = useCase.execute(command(Set.of(2)));

        assertThat(report.rows()).singleElement().satisfies(result -> {
            assertThat(result.status()).isEqualTo(StockReceiptImportRowExecutionStatus.FAILED);
            assertThat(result.issues()).singleElement()
                    .extracting(StockReceiptImportIssue::code)
                    .isEqualTo(StockReceiptImportIssueCode.EXECUTION_FAILED);
        });
        assertThat(report.summary().failedRows()).isEqualTo(1);
        verify(failureReporter).report(importId, 2, failure);
        verify(ledger).saveRowResult(eq(importId), any(StockReceiptImportRowExecutionResult.class));
    }

    @Test
    void shouldRejectSelectionContainingALineOutsideTheFile() {
        givenPreview(existingRow(2, "REF-2", "Plaquette", 4));

        assertThatThrownBy(() -> useCase.execute(command(Set.of(99))))
                .isInstanceOf(InvalidStockReceiptImportSelectionException.class)
                .hasMessageContaining("99");

        verify(ledger, never()).ensureExecution(any());
    }

    @Test
    void shouldFingerprintFileContextAndSelectionDeterministically() {
        StockReceiptImportExecutionFingerprint fingerprint = new StockReceiptImportExecutionFingerprint();
        StockReceiptImportExecutionCommand first = command(Set.of(3, 2));
        StockReceiptImportExecutionCommand same = command(Set.of(2, 3));
        StockReceiptImportExecutionCommand anotherSelection = command(Set.of(2));

        assertThat(fingerprint.calculate(first)).isEqualTo(fingerprint.calculate(same));
        assertThat(fingerprint.calculate(first)).hasSize(64);
        assertThat(fingerprint.calculate(first)).isNotEqualTo(fingerprint.calculate(anotherSelection));
    }

    private StockReceiptImportExecutionCommand command(Set<Integer> selectedLines) {
        return new StockReceiptImportExecutionCommand(importId, file, shopId, userId, selectedLines);
    }

    private void givenPreview(StockReceiptImportRowPreview... rows) {
        int creations = (int) List.of(rows).stream()
                .filter(row -> row.action() == StockReceiptImportRowAction.CREATE_PRODUCT).count();
        int existing = (int) List.of(rows).stream()
                .filter(row -> row.action() == StockReceiptImportRowAction.RECEIVE_EXISTING).count();
        int invalid = (int) List.of(rows).stream()
                .filter(row -> row.action() == StockReceiptImportRowAction.REJECT).count();
        when(prepareUseCase.execute(file, shopId)).thenReturn(new StockReceiptImportPreview(
                List.of(rows), new StockReceiptImportSummary(rows.length, creations, existing, invalid)));
    }

    private static StockReceiptImportRowPreview creationRow(
            int lineNumber,
            String reference,
            String name,
            int shopFloorQuantity,
            int backstockQuantity
    ) {
        return new StockReceiptImportRowPreview(
                lineNumber,
                StockReceiptImportRowAction.CREATE_PRODUCT,
                reference,
                name,
                "Filtres",
                CategoryId.generate(),
                Money.create(new BigDecimal("12500"), XAF),
                2,
                null,
                List.of(
                        new TargetLocation(LocationId.generate(), shopFloorQuantity),
                        new TargetLocation(LocationId.generate(), backstockQuantity)
                ),
                List.of()
        );
    }

    private static StockReceiptImportRowPreview existingRow(
            int lineNumber,
            String reference,
            String name,
            int quantity
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
                List.of(new TargetLocation(LocationId.generate(), quantity)),
                List.of()
        );
    }

    private static StockReceiptImportRowPreview invalidRow(int lineNumber, String reference) {
        return new StockReceiptImportRowPreview(
                lineNumber,
                StockReceiptImportRowAction.REJECT,
                reference,
                "",
                "",
                null,
                null,
                null,
                null,
                List.of(),
                List.of(new StockReceiptImportIssue(
                        "nom_produit",
                        StockReceiptImportIssueCode.MISSING_NAME,
                        "Product name is required."
                ))
        );
    }

    private static ReceiveStockResult receipt(ProductId productId, int totalReceived) {
        return new ReceiveStockResult(productId, totalReceived, List.of(), Instant.parse("2026-08-05T10:00:00Z"));
    }
}
