package com.aliCheikh.stock.application.dto;

import java.util.Objects;

/** Ligne brute lue par un adaptateur de format (CSV aujourd'hui, XLSX plus tard). */
public record StockReceiptImportRowData(
        int lineNumber,
        String reference,
        String name,
        String categoryName,
        String unitPriceAmount,
        String minimumGlobalThreshold,
        String shopFloorQuantity,
        String backstockQuantity
) {
    public StockReceiptImportRowData {
        if (lineNumber < 2) {
            throw new IllegalArgumentException("lineNumber must point after the header");
        }
        Objects.requireNonNull(reference, "reference cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(categoryName, "categoryName cannot be null");
        Objects.requireNonNull(unitPriceAmount, "unitPriceAmount cannot be null");
        Objects.requireNonNull(minimumGlobalThreshold, "minimumGlobalThreshold cannot be null");
        Objects.requireNonNull(shopFloorQuantity, "shopFloorQuantity cannot be null");
        Objects.requireNonNull(backstockQuantity, "backstockQuantity cannot be null");
    }
}
