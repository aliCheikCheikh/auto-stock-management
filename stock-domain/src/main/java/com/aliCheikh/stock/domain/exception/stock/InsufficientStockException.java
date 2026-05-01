package com.aliCheikh.stock.domain.exception.stock;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.product.ProductId;

public class InsufficientStockException extends DomainException {

    private final ProductId productId;
    private final int availableQuantity;
    private final int requestedQuantity;

    public InsufficientStockException(ProductId productId, int availableQuantity, int requestedQuantity) {
        // Le message formaté reste très utile pour les logs backend
        super(String.format("Insufficient stock for product %s: available = %d, requested = %d",
                productId, availableQuantity, requestedQuantity));

        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
    }

    public ProductId getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}