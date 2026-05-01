package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidProductNameException extends DomainException {
    private final String invalidProductName;

    public InvalidProductNameException(String invalidProductName) {
        // Règle : Le nom ne doit pas être vide ou null.
        super("Product name cannot be null or blank. Provided value: '" + invalidProductName + "'");
        this.invalidProductName = invalidProductName;
    }

    public String getInvalidProductName() {
        return invalidProductName;
    }
}