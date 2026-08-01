package com.aliCheikh.stock.application.dto;

import java.util.Objects;
import java.util.UUID;

/**
 * Les critères de consultation des créances.
 *
 * <p>Aucun critère de tri n'est exposé : l'ordre utile dépend du statut demandé et relève d'une
 * décision métier, pas d'un réglage d'écran. Une créance en cours se lit de la plus ancienne à la
 * plus récente — c'est celle-là qu'on relance ; une créance soldée se lit du règlement le plus
 * récent au plus ancien. Laisser l'appelant en décider reviendrait à laisser chaque écran inventer
 * sa propre urgence.</p>
 *
 * @param customerId restreint à un client, ou {@code null} pour toute la boutique
 */
public record ListDebtsQuery(int page,
                             int size,
                             DebtStatus status,
                             UUID customerId) {

    /** Borne haute alignée sur les autres consultations paginées du système. */
    public static final int MAX_PAGE_SIZE = 200;

    public ListDebtsQuery {
        Objects.requireNonNull(status, "status cannot be null");

        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
