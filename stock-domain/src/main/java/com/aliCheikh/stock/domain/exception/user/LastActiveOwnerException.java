package com.aliCheikh.stock.domain.exception.user;

import com.aliCheikh.stock.domain.exception.DomainException;

public final class LastActiveOwnerException extends DomainException {

    public LastActiveOwnerException() {
        super("The shop must retain at least one active owner.");
    }
}
