package com.aliCheikh.stock.domain.exception.shop;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidShopAddressException extends DomainException {
    private final String invalidShopAddress;

    public InvalidShopAddressException(String invalidShopAddress) {
        super("Shop address cannot be null or blank. Provided value: '" + invalidShopAddress + "'");
        this.invalidShopAddress = invalidShopAddress; // CORRIGÉ
    }

    public String getInvalidShopAddress() {
        return invalidShopAddress;
    }
}