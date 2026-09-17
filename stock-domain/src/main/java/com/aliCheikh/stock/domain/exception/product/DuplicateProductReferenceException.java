package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;

public class DuplicateProductReferenceException extends DomainException {
    private final String duplicatedReference;

    public DuplicateProductReferenceException(String reference) {
        super("A product with reference '" + reference + "' already exists.");
        duplicatedReference = reference;
    }

    public String getDuplicatedReference() {
        return duplicatedReference;
    }
}
