package com.aliCheikh.stock.domain.exception.shop;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidShopNameException extends DomainException {
    private final String invalidShopName;

    public InvalidShopNameException(String invalidShopName) {
        super("Shop name cannot be null or blank. Provided value: '" + invalidShopName + "'");
        this.invalidShopName = invalidShopName;
    }

    public String getInvalidShopName() {
        return invalidShopName;
    }
}
