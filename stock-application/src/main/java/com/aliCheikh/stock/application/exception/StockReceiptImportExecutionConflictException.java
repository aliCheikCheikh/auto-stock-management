package com.aliCheikh.stock.application.exception;

import java.util.Objects;
import java.util.UUID;

public final class StockReceiptImportExecutionConflictException extends RuntimeException {

    private final UUID importId;

    public StockReceiptImportExecutionConflictException(UUID importId) {
        super("This import ID has already been used with a different file or selection.");
        this.importId = Objects.requireNonNull(importId, "importId cannot be null");
    }

    public UUID importId() {
        return importId;
    }
}
