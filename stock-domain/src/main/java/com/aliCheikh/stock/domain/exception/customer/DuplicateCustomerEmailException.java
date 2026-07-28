package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

/** Levée quand une adresse email est déjà rattachée à un autre client. */
public class DuplicateCustomerEmailException extends DomainException {

    private final String duplicatedEmail;

    public DuplicateCustomerEmailException(String duplicatedEmail) {
        super("A customer already exists with the email " + duplicatedEmail);
        this.duplicatedEmail = duplicatedEmail;
    }

    public String getDuplicatedEmail() {
        return duplicatedEmail;
    }
}
