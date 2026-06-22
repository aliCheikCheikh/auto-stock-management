package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.product.ProductId;

public class InactiveProductException extends DomainException {
    private final ProductId inactiveProductId;

    public InactiveProductException(ProductId productId) {
        super("Product '" + productId + "' is inactive and cannot be used for this operation.");
        this.inactiveProductId = productId;
    }

    public ProductId getInactiveProductId() {
        return inactiveProductId;
    }
}
