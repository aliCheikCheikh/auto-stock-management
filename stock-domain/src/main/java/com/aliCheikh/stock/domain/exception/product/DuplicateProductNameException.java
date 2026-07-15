package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;

public class DuplicateProductNameException extends DomainException {
    private final String duplicatedName;

    public DuplicateProductNameException(String name) {
        super("Le produit avec le nom : " + " " + name + " exite déjà dans le catalogue");
        duplicatedName = name;
    }

    public String getDuplicatedName() {
        return duplicatedName;
    }
}
