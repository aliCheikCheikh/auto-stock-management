package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class LastActiveOwnerException extends DomainException {

    public LastActiveOwnerException() {
        super("Le magasin doit toujours conserver au moins un propriétaire actif.");
    }
}
