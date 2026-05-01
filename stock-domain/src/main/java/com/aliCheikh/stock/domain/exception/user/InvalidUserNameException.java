package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidUserNameException extends DomainException {
    private final String invalidUserName;

    public InvalidUserNameException(String userName) {
        super("User name cannot be null or blank. Provided value: '" + userName + "'");
        this.invalidUserName = userName;
    }

    public String getInvalidUserName() {
        return invalidUserName;
    }
}