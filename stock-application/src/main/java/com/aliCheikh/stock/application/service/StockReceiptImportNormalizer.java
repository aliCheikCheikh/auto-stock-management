package com.aliCheikh.stock.application.service;

import java.util.Locale;
import java.util.Objects;

public final class StockReceiptImportNormalizer {

    private StockReceiptImportNormalizer() {
    }

    public static String normalizeKey(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
