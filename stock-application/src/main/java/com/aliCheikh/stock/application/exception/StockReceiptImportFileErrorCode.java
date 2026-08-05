package com.aliCheikh.stock.application.exception;

public enum StockReceiptImportFileErrorCode {
    UNSUPPORTED_FILE,
    FILE_TOO_LARGE,
    INVALID_ENCODING,
    INVALID_HEADER,
    EMPTY_FILE,
    TOO_MANY_ROWS,
    MALFORMED_CSV
}
