package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ProductSearchView;
import com.aliCheikh.stock.application.port.ProductSearchQueryPort;

import java.util.List;
import java.util.Objects;

public class SearchProductsUseCase {
    private final ProductSearchQueryPort productSearchQueryPort;
    private static final int MAX_RESULTS = 10;

    public SearchProductsUseCase(ProductSearchQueryPort productSearchQueryPort) {
        this.productSearchQueryPort = Objects.requireNonNull(productSearchQueryPort, "productSearchQueryPort cannot be null");
    }

    public List<ProductSearchView> findProductsByKeyword(String keyword) {
        if (keyword == null) {
            return List.of();
        }

        String trimmed = keyword.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }

        return productSearchQueryPort.findProductsByKeyword(trimmed, MAX_RESULTS);
    }
}
