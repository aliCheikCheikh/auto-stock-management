package com.aliCheikh.stock.application.exception;

import java.util.List;

public final class InvalidStockReceiptImportSelectionException extends RuntimeException {

    private final List<Integer> unknownLineNumbers;

    public InvalidStockReceiptImportSelectionException(List<Integer> unknownLineNumbers) {
        super("Selected rows do not exist in the file: " + unknownLineNumbers + ".");
        this.unknownLineNumbers = List.copyOf(unknownLineNumbers);
    }

    public List<Integer> unknownLineNumbers() {
        return unknownLineNumbers;
    }
}
