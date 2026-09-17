package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

/** Raised when the required customer given name is missing or blank. */
public class InvalidCustomerGivenNameException extends DomainException {

    private final String invalidGivenName;

    public InvalidCustomerGivenNameException(String invalidGivenName) {
        super("Invalid customer given name: \"" + invalidGivenName + "\" (the value must not be null or blank)");
        this.invalidGivenName = invalidGivenName;
    }

    public String getInvalidGivenName() {
        return invalidGivenName;
    }
}
