package com.aliCheikh.stock.application.dto;

import java.util.Objects;

/**
 * Customer registration request. {@code PhoneNumber} normalizes the raw phone input. Father name
 * and email are optional.
 */
public record RegisterCustomerCommand(String rawPhoneNumber,
                                      String givenName,
                                      String fatherName,
                                      String email) {

    public RegisterCustomerCommand {
        Objects.requireNonNull(rawPhoneNumber, "rawPhoneNumber cannot be null");
        Objects.requireNonNull(givenName, "givenName cannot be null");
    }
}
