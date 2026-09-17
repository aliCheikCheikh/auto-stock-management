package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class IncorrectCurrentPasswordException extends DomainException {

    public IncorrectCurrentPasswordException() {
        super("The current password is incorrect.");
    }
}
