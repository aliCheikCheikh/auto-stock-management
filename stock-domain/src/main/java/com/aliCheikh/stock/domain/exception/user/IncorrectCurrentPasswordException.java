package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class IncorrectCurrentPasswordException extends DomainException {

    public IncorrectCurrentPasswordException() {
        super("Le mot de passe actuel est incorrect.");
    }
}
