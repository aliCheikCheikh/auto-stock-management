package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;

/** Raised when a category name already exists, ignoring case. */
public class DuplicateCategoryNameException extends DomainException {

    private final String duplicatedName;

    public DuplicateCategoryNameException(String duplicatedName) {
        super("A category named \"" + duplicatedName + "\" already exists");
        this.duplicatedName = duplicatedName;
    }

    public String getDuplicatedName() {
        return duplicatedName;
    }
}
