package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class InvalidUserEmailException extends DomainException {

    private final String invalidEmail;

    public InvalidUserEmailException(String invalidEmail) {
        super("User email address is invalid.");
        this.invalidEmail = invalidEmail;
    }

    public String getInvalidEmail() {
        return invalidEmail;
    }
}
