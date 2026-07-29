package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidUserNameException extends DomainException {
    private final String invalidUserName;

    public InvalidUserNameException(String userName, int maximumLength) {
        super("Le nom affiché d'un utilisateur est obligatoire et ne peut pas dépasser "
                + maximumLength + " caractères.");
        this.invalidUserName = userName;
    }

    public String getInvalidUserName() {
        return invalidUserName;
    }
}
