package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(int minimumLength) {
        super("Le mot de passe doit contenir au moins " + minimumLength + " caractères.");
    }
}
