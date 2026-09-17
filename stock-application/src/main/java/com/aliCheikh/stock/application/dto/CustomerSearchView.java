package com.aliCheikh.stock.application.dto;

import java.util.UUID;

/** Customer search result with the fields needed to identify a customer at checkout. */
public record CustomerSearchView(UUID customerId,
                                 String givenName,
                                 String fatherName,
                                 String phoneNumber) {
}
