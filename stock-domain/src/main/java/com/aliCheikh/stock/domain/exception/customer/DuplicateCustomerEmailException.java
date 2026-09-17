package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

/** Raised when an email address already belongs to another customer. */
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
