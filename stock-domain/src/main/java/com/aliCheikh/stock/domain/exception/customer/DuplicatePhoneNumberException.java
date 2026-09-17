package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;

/** Raised when a phone number already belongs to another customer. */
public class DuplicatePhoneNumberException extends DomainException {

    private final PhoneNumber duplicatedPhoneNumber;

    public DuplicatePhoneNumberException(PhoneNumber duplicatedPhoneNumber) {
        super("A customer already exists with the phone number " + duplicatedPhoneNumber);
        this.duplicatedPhoneNumber = duplicatedPhoneNumber;
    }

    public PhoneNumber getDuplicatedPhoneNumber() {
        return duplicatedPhoneNumber;
    }
}
