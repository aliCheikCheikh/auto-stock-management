package com.aliCheikh.stock.application.exception;

import java.util.Objects;
import java.util.UUID;

public final class StockReceiptImportExecutionConflictException extends RuntimeException {

    private final UUID importId;

    public StockReceiptImportExecutionConflictException(UUID importId) {
        super("Cet identifiant d'import a déjà été utilisé avec un autre fichier ou une autre sélection.");
        this.importId = Objects.requireNonNull(importId, "importId cannot be null");
    }

    public UUID importId() {
        return importId;
    }
}
