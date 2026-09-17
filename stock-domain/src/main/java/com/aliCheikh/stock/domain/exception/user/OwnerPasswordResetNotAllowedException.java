package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.user.UserId;

public final class OwnerPasswordResetNotAllowedException extends DomainException {

    private final UserId ownerId;

    public OwnerPasswordResetNotAllowedException(UserId ownerId) {
        super("Temporary password resets are only available for sellers.");
        this.ownerId = ownerId;
    }

    public UserId getOwnerId() {
        return ownerId;
    }
}
