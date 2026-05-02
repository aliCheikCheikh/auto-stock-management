package com.aliCheikh.stock.domain.exception.movement;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidMovementException extends DomainException {
    private final MovementErrorReason movementErrorReason;

    public InvalidMovementException(MovementErrorReason movementErrorReason, String message) {
        super(message);
        this.movementErrorReason = movementErrorReason;
    }

    public MovementErrorReason getMovementErrorReason() {
        return movementErrorReason;
    }

}
