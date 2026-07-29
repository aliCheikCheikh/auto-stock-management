package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.user.UserEmail;

public final class DuplicateUserEmailException extends DomainException {

    private final UserEmail duplicatedEmail;

    public DuplicateUserEmailException(UserEmail duplicatedEmail) {
        super("Un utilisateur possède déjà cette adresse email.");
        this.duplicatedEmail = duplicatedEmail;
    }

    public UserEmail getDuplicatedEmail() {
        return duplicatedEmail;
    }
}
