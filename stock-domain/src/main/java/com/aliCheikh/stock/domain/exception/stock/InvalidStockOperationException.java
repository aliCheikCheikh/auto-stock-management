package com.aliCheikh.stock.domain.exception.stock;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.product.ProductId;

public class InvalidStockOperationException extends DomainException {
    private final ProductId productId;
    private final int currentQuantity;
    private final int invalidQuantity; // Renommé pour être générique (increase ou decrease)

    public InvalidStockOperationException(ProductId productId, int currentQuantity, int invalidQuantity) {
        super(String.format("Invalid stock operation for product %s. Current quantity: %d, Invalid input: %d",
                productId, currentQuantity, invalidQuantity));
        this.productId = productId;
        this.currentQuantity = currentQuantity;
        this.invalidQuantity = invalidQuantity;
    }

    public ProductId getProductId() {
        return productId;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public int getInvalidQuantity() {
        return invalidQuantity;
    }
}