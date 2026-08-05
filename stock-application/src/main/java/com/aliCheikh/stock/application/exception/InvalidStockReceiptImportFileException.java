package com.aliCheikh.stock.application.exception;

import java.util.Objects;

public final class InvalidStockReceiptImportFileException extends RuntimeException {

    private final StockReceiptImportFileErrorCode code;

    public InvalidStockReceiptImportFileException(StockReceiptImportFileErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code cannot be null");
    }

    public InvalidStockReceiptImportFileException(
            StockReceiptImportFileErrorCode code,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code cannot be null");
    }

    public StockReceiptImportFileErrorCode code() {
        return code;
    }
}
