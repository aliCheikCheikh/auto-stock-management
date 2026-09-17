package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

/** Raised when input cannot be normalized to a valid phone number. */
public class InvalidPhoneNumberException extends DomainException {

    private final String rawPhoneNumber;

    public InvalidPhoneNumberException(String rawPhoneNumber, String reason) {
        super("Invalid phone number \"" + rawPhoneNumber + "\": " + reason);
        this.rawPhoneNumber = rawPhoneNumber;
    }

    /** Original input as supplied by the user. */
    public String getRawPhoneNumber() {
        return rawPhoneNumber;
    }
}
