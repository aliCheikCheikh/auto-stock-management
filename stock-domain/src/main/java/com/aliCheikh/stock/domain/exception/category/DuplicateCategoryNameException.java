package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;

/** Levée quand une catégorie portant déjà ce nom existe, à la casse près. */
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
