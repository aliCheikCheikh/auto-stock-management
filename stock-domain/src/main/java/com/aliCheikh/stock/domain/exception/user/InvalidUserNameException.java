package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidUserNameException extends DomainException {
    private final String invalidUserName;

    public InvalidUserNameException(String userName, int maximumLength) {
        super("User display name is required and must not exceed "
                + maximumLength + " characters.");
        this.invalidUserName = userName;
    }

    public String getInvalidUserName() {
        return invalidUserName;
    }
}
