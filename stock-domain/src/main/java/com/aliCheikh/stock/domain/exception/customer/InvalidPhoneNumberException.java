package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidPhoneNumberException extends DomainException {
    private final String invalidPhoneNumber;

    public InvalidPhoneNumberException(String phoneNumber) {
        super("Invalid phone number: " + phoneNumber);
        this.invalidPhoneNumber = phoneNumber;
    }

    public String getInvalidPhoneNumber() {
        return invalidPhoneNumber;
    }
}
