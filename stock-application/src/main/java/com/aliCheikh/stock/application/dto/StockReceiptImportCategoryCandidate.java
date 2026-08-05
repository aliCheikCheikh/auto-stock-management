package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.category.CategoryId;

import java.util.Objects;

public record StockReceiptImportCategoryCandidate(CategoryId categoryId, String name) {
    public StockReceiptImportCategoryCandidate {
        Objects.requireNonNull(categoryId, "categoryId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
    }
}
