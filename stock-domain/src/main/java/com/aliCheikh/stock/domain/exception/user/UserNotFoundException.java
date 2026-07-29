package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.user.UserId;

public final class UserNotFoundException extends DomainException {

    private final UserId userId;

    public UserNotFoundException(UserId userId) {
        super("L'utilisateur demandé n'existe pas.");
        this.userId = userId;
    }

    public UserId getUserId() {
        return userId;
    }
}
