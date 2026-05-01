package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;

public record SaleLineInput(ProductId productId, int  quantity, Money unitPrice) {
}
