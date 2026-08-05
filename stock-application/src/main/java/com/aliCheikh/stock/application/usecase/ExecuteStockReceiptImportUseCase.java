package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ProductInfo;
import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.ReceiveStockResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionCommand;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionDescriptor;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionReport;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionSummary;
import com.aliCheikh.stock.application.dto.StockReceiptImportPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowPreview;
import com.aliCheikh.stock.application.exception.InvalidStockReceiptImportSelectionException;
import com.aliCheikh.stock.application.port.StockReceiptImportExecutionLedger;
import com.aliCheikh.stock.application.port.StockReceiptImportFailureReporter;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.service.StockReceiptImportExecutionFingerprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Exécute les lignes choisies avec une transaction et une preuve d'idempotence durables par ligne. */
public class ExecuteStockReceiptImportUseCase {

    private final PrepareStockReceiptImportUseCase prepareUseCase;
    private final ReceiveStockUseCase receiveStockUseCase;
    private final StockReceiptImportExecutionLedger ledger;
    private final StockReceiptImportFailureReporter failureReporter;
    private final TransactionRunner transactionRunner;
    private final StockReceiptImportExecutionFingerprint fingerprint;

    public ExecuteStockReceiptImportUseCase(
            PrepareStockReceiptImportUseCase prepareUseCase,
            ReceiveStockUseCase receiveStockUseCase,
            StockReceiptImportExecutionLedger ledger,
            StockReceiptImportFailureReporter failureReporter,
            TransactionRunner transactionRunner,
            StockReceiptImportExecutionFingerprint fingerprint
    ) {
        this.prepareUseCase = Objects.requireNonNull(prepareUseCase, "prepareUseCase cannot be null");
        this.receiveStockUseCase = Objects.requireNonNull(receiveStockUseCase, "receiveStockUseCase cannot be null");
        this.ledger = Objects.requireNonNull(ledger, "ledger cannot be null");
        this.failureReporter = Objects.requireNonNull(failureReporter, "failureReporter cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint cannot be null");
    }

    public StockReceiptImportExecutionReport execute(StockReceiptImportExecutionCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        StockReceiptImportPreview preview = prepareUseCase.execute(command.file(), command.shopId());
        validateSelection(command.selectedLineNumbers(), preview.rows());
        ledger.ensureExecution(new StockReceiptImportExecutionDescriptor(
                command.importId(), fingerprint.calculate(command), command.shopId(), command.userId()));

        List<StockReceiptImportRowExecutionResult> results = new ArrayList<>(preview.rows().size());
        for (StockReceiptImportRowPreview row : preview.rows()) {
            results.add(executeOrIgnore(command, row));
        }

        List<StockReceiptImportRowExecutionResult> immutableResults = List.copyOf(results);
        StockReceiptImportExecutionSummary summary = StockReceiptImportExecutionSummary.from(
                immutableResults, command.selectedLineNumbers().size());
        return new StockReceiptImportExecutionReport(command.importId(), immutableResults, summary);
    }

    private StockReceiptImportRowExecutionResult executeOrIgnore(
            StockReceiptImportExecutionCommand command,
            StockReceiptImportRowPreview row
    ) {
        if (row.action() == StockReceiptImportRowAction.REJECT) {
            return StockReceiptImportRowExecutionResult.ignoredInvalid(row);
        }
        if (!command.selectedLineNumbers().contains(row.lineNumber())) {
            return StockReceiptImportRowExecutionResult.ignoredByUser(row);
        }
        return executeSelectedRow(command, row);
    }

    private StockReceiptImportRowExecutionResult executeSelectedRow(
            StockReceiptImportExecutionCommand command,
            StockReceiptImportRowPreview row
    ) {
        try {
            return transactionRunner.execute(() -> {
                ledger.lockExecution(command.importId());
                Optional<StockReceiptImportRowExecutionResult> previous =
                        ledger.findRowResult(command.importId(), row.lineNumber());
                if (previous.isPresent()) {
                    return previous.get();
                }

                ReceiveStockResult receipt = receiveStockUseCase.execute(toReceiveStockCommand(command, row));
                StockReceiptImportRowExecutionResult imported = StockReceiptImportRowExecutionResult.imported(
                        row.lineNumber(), row.action(), row.reference(), row.name(),
                        receipt.productId(), receipt.totalReceived());
                ledger.saveRowResult(command.importId(), imported);
                return imported;
            });
        } catch (RuntimeException failure) {
            failureReporter.report(command.importId(), row.lineNumber(), failure);
            return persistFailureUnlessAlreadyCompleted(command.importId(), row);
        }
    }

    private StockReceiptImportRowExecutionResult persistFailureUnlessAlreadyCompleted(
            UUID importId,
            StockReceiptImportRowPreview row
    ) {
        return transactionRunner.execute(() -> {
            ledger.lockExecution(importId);
            Optional<StockReceiptImportRowExecutionResult> previous =
                    ledger.findRowResult(importId, row.lineNumber());
            if (previous.isPresent()) {
                return previous.get();
            }
            StockReceiptImportRowExecutionResult failed = StockReceiptImportRowExecutionResult.failed(row);
            ledger.saveRowResult(importId, failed);
            return failed;
        });
    }

    private ReceiveStockCommand toReceiveStockCommand(
            StockReceiptImportExecutionCommand command,
            StockReceiptImportRowPreview row
    ) {
        ProductInfo productInfo = row.action() == StockReceiptImportRowAction.CREATE_PRODUCT
                ? new ProductInfo(
                        row.name(), row.reference(), row.categoryId(), row.unitPrice(),
                        row.minimumGlobalThreshold())
                : null;
        return new ReceiveStockCommand(
                row.reference(), productInfo, command.shopId(), command.userId(), row.distributions());
    }

    private void validateSelection(Set<Integer> selectedLines, List<StockReceiptImportRowPreview> rows) {
        Set<Integer> fileLines = rows.stream()
                .map(StockReceiptImportRowPreview::lineNumber)
                .collect(Collectors.toUnmodifiableSet());
        List<Integer> unknownLines = selectedLines.stream()
                .filter(lineNumber -> !fileLines.contains(lineNumber))
                .sorted()
                .toList();
        if (!unknownLines.isEmpty()) {
            throw new InvalidStockReceiptImportSelectionException(unknownLines);
        }
    }
}
