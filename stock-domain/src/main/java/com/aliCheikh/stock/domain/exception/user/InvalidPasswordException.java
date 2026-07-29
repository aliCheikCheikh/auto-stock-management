package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(int minimumLength, int maximumLength) {
        super("Le mot de passe doit contenir entre "
                + minimumLength + " et " + maximumLength + " caractères.");
    }
}
