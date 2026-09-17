package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.domain.model.customer.Customer;

import java.util.UUID;

/** Customer response exposing the canonical phone number. */
public record CustomerResponse(UUID customerId,
                               String givenName,
                               String fatherName,
                               String phoneNumber,
                               String email) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId().getValue(),
                customer.getGivenName(),
                customer.getFatherName().orElse(null),
                customer.getPhoneNumber().getValue(),
                customer.getEmail().orElse(null)
        );
    }
}
