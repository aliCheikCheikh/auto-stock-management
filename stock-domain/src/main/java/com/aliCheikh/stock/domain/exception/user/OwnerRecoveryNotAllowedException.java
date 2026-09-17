package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.user.UserEmail;

public final class OwnerRecoveryNotAllowedException extends DomainException {

    private final UserEmail email;

    public OwnerRecoveryNotAllowedException(UserEmail email) {
        super("Recovery of this owner account is not allowed.");
        this.email = email;
    }

    public UserEmail getEmail() {
        return email;
    }
}
