package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.shared.Money;

public class InvalidProductPriceException extends DomainException {
    private final Money invalidPrice;

    public InvalidProductPriceException(Money invalidPrice) {
        // Règle : Le prix doit être strictement positif.
        super("Product unit price must be strictly positive. Provided value: '" + invalidPrice + "'");
        this.invalidPrice = invalidPrice;
    }

    public Money getInvalidPrice() {
        return invalidPrice;
    }
}