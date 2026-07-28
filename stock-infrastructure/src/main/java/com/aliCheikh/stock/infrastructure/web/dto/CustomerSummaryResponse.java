package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.CustomerSearchView;

import java.util.UUID;

/** Client réduit aux informations d'un résultat de recherche. */
public record CustomerSummaryResponse(UUID customerId,
                                      String givenName,
                                      String fatherName,
                                      String phoneNumber) {

    public static CustomerSummaryResponse from(CustomerSearchView view) {
        return new CustomerSummaryResponse(
                view.customerId(),
                view.givenName(),
                view.fatherName(),
                view.phoneNumber());
    }
}
