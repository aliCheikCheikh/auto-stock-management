package com.aliCheikh.stock.application.dto;

import java.util.Objects;

/**
 * Demande d'enregistrement d'un client.
 *
 * <p>Le téléphone arrive ici en texte brut, tel que saisi par le vendeur : c'est le Value Object
 * {@code PhoneNumber} qui le normalisera. Prénom (nom du père) et email sont facultatifs.</p>
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
