package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Demande d'enregistrement d'un client.
 *
 * <p>Le téléphone est transmis tel que saisi : sa normalisation est l'affaire du domaine, pas du
 * web. Le nom du père et l'email sont facultatifs.</p>
 */
public record CreateCustomerRequest(@NotBlank @Size(max = 100) String givenName,
                                    @Size(max = 100) String fatherName,
                                    @NotBlank @Size(max = 30) String phoneNumber,
                                    @Email @Size(max = 200) String email) {
}
