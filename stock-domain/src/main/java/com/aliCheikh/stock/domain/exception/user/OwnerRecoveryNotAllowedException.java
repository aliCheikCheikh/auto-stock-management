package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.user.UserEmail;

public final class OwnerRecoveryNotAllowedException extends DomainException {

    private final UserEmail email;

    public OwnerRecoveryNotAllowedException(UserEmail email) {
        super("La récupération de cet accès propriétaire n'est pas autorisée.");
        this.email = email;
    }

    public UserEmail getEmail() {
        return email;
    }
}
