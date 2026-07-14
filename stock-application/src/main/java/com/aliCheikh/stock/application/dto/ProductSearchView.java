package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;

public record ProductSearchView(ProductId productId,
                                String name,
                                String reference,
                                Money unitPrice) {
}
