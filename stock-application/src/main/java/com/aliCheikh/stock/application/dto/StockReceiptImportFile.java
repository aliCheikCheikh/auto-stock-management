package com.aliCheikh.stock.application.dto;

import java.util.Arrays;
import java.util.Objects;

/** Contenu borné par l'adaptateur entrant avant d'atteindre le cas d'usage. */
public record StockReceiptImportFile(String filename, byte[] content) {

    public StockReceiptImportFile {
        Objects.requireNonNull(filename, "filename cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        if (filename.isBlank()) {
            throw new IllegalArgumentException("filename cannot be blank");
        }
        if (content.length == 0) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
