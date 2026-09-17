package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidThresholdException extends DomainException {
    private final int invalidThreshold;

    public InvalidThresholdException(int invalidThreshold) {

        super("Minimum global threshold cannot be negative. Provided value: '" + invalidThreshold + "'");
        this.invalidThreshold = invalidThreshold;
    }

    public int getInvalidThreshold() {
        return invalidThreshold;
    }
}
