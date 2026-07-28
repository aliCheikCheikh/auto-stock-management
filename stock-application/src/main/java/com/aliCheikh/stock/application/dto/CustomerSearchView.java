package com.aliCheikh.stock.application.dto;

import java.util.UUID;

/**
 * Client tel qu'affiché dans un sélecteur de recherche.
 *
 * <p>Modèle de lecture volontairement réduit : le vendeur a besoin d'identifier la personne
 * (nom, nom du père) et de la reconnaître par son numéro. Rien de plus.</p>
 */
public record CustomerSearchView(UUID customerId,
                                 String givenName,
                                 String fatherName,
                                 String phoneNumber) {
}
