package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidCustomerEmailException extends DomainException {
    private final String invalidEmail;

    public InvalidCustomerEmailException(String invalidEmail) {
        super("Invalid email address : " + invalidEmail);
        this.invalidEmail = invalidEmail;
    }

    public String getInvalidEmail() {
        return invalidEmail;
    }
}
