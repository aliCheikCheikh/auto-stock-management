package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.shared.Money;

public record ProductInfo(
        String name,
        String reference,
        CategoryId categoryId,
        Money unitPrice,
        int minimumGlobalThreshold
) {
}