package com.aliCheikh.stock.domain.exception.movement;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidQuantityMovementException extends DomainException {
    private final int faultQuantity;

    public InvalidQuantityMovementException(int faultQuantity) {
        super("The quantity of a stock movement must be strictly positive. Received value: " + faultQuantity);
        this.faultQuantity = faultQuantity;
    }

    public int getFaultQuantity() {
        return faultQuantity;
    }
}
