package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionDescriptor;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;

import java.util.Optional;
import java.util.UUID;

public interface StockReceiptImportExecutionLedger {

    /** Crée l'exécution si nécessaire ou vérifie que cet identifiant désigne exactement la même demande. */
    void ensureExecution(StockReceiptImportExecutionDescriptor descriptor);

    /** Verrouille l'exécution dans la transaction courante avant toute écriture de stock. */
    void lockExecution(UUID importId);

    Optional<StockReceiptImportRowExecutionResult> findRowResult(UUID importId, int lineNumber);

    void saveRowResult(UUID importId, StockReceiptImportRowExecutionResult result);
}
