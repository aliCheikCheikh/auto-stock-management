package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionDescriptor;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;

import java.util.Optional;
import java.util.UUID;

public interface StockReceiptImportExecutionLedger {

    /** Creates the execution or verifies that the ID refers to the same request. */
    void ensureExecution(StockReceiptImportExecutionDescriptor descriptor);

    /** Locks the execution in the current transaction before any stock write. */
    void lockExecution(UUID importId);

    Optional<StockReceiptImportRowExecutionResult> findRowResult(UUID importId, int lineNumber);

    void saveRowResult(UUID importId, StockReceiptImportRowExecutionResult result);
}
