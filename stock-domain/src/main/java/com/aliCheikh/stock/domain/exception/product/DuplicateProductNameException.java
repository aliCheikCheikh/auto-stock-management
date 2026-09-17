package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;

public class DuplicateProductNameException extends DomainException {
    private final String duplicatedName;

    public DuplicateProductNameException(String name) {
        super("A product named '" + name + "' already exists in the catalog.");
        duplicatedName = name;
    }

    public String getDuplicatedName() {
        return duplicatedName;
    }
}
