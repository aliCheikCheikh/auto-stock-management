package com.aliCheikh.stock.domain.exception.stock;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidStorageLocationLabelException extends DomainException {
    private final String invalidLabel;
    public InvalidStorageLocationLabelException(String invalidLabel) {
        super(String.format("Invalid storage location label: %s", invalidLabel));
        this.invalidLabel = invalidLabel;
    }
    public String getInvalidLabel() {
        return invalidLabel;
    }

}
