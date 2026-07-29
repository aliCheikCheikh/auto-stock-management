package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.user.UserId;

public final class OwnerPasswordResetNotAllowedException extends DomainException {

    private final UserId ownerId;

    public OwnerPasswordResetNotAllowedException(UserId ownerId) {
        super("Le mot de passe d'un propriétaire ne peut être réinitialisé que par la procédure de secours du serveur.");
        this.ownerId = ownerId;
    }

    public UserId getOwnerId() {
        return ownerId;
    }
}
