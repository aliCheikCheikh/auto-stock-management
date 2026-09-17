package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidCategoryNameException extends DomainException {

    public InvalidCategoryNameException(String invalidName) {

        super("Category name cannot be null or blank. Provided value: '" + invalidName + "'");
    }
}
