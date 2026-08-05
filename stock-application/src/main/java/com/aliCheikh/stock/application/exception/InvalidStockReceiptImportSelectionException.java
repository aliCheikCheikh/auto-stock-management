package com.aliCheikh.stock.application.exception;

import java.util.List;

public final class InvalidStockReceiptImportSelectionException extends RuntimeException {

    private final List<Integer> unknownLineNumbers;

    public InvalidStockReceiptImportSelectionException(List<Integer> unknownLineNumbers) {
        super("Les lignes sélectionnées n'existent pas dans le fichier : " + unknownLineNumbers + ".");
        this.unknownLineNumbers = List.copyOf(unknownLineNumbers);
    }

    public List<Integer> unknownLineNumbers() {
        return unknownLineNumbers;
    }
}
