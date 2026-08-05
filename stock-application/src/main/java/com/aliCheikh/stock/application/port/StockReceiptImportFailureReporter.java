package com.aliCheikh.stock.application.port;

import java.util.UUID;

public interface StockReceiptImportFailureReporter {
    void report(UUID importId, int lineNumber, RuntimeException failure);
}
