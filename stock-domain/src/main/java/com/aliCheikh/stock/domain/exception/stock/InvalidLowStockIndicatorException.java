package com.aliCheikh.stock.domain.exception.stock;

import com.aliCheikh.stock.domain.exception.DomainException;


public class InvalidLowStockIndicatorException extends DomainException {

    private final int invalidIndicator;

    public InvalidLowStockIndicatorException(int invalidIndicator) {
        super("Low stock indicator cannot be negative. Received: " + invalidIndicator);
        this.invalidIndicator = invalidIndicator;
    }

    public int getInvalidIndicator() {
        return invalidIndicator;
    }
}
