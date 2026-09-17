package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidProductReferenceException extends DomainException {
    private final String invalidProductReference;

    public InvalidProductReferenceException(String invalidProductReference) {

        super("Product reference cannot be null or blank. Provided value: '" + invalidProductReference + "'");
        this.invalidProductReference = invalidProductReference;
    }

    public String getInvalidProductName() {
        return invalidProductReference;
    }
}
