package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(int minimumLength, int maximumLength) {
        super("Password must contain between "
                + minimumLength + " and " + maximumLength + " characters.");
    }
}
