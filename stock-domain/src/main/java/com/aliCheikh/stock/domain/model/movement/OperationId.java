package com.aliCheikh.stock.domain.model.movement;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifiant de l'opération à l'origine d'un ensemble de mouvements de stock.
 *
 * <p>Une réception, un transfert ou une vente produisent chacun <b>plusieurs</b> mouvements : un par
 * produit, parfois un par emplacement. Sans marqueur commun, l'historique les présente comme autant
 * d'événements indépendants, et rien ne distingue trois lignes d'une même réception de trois
 * réceptions séparées.</p>
 *
 * <p>Regrouper d'après la date et l'auteur serait une heuristique : deux opérations rapprochées du
 * même vendeur seraient fusionnées à tort. L'identité est donc posée explicitement au moment où
 * l'opération est décidée.</p>
 */
public final class OperationId {

    private final UUID value;

    private OperationId(UUID value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    public static OperationId generate() {
        return new OperationId(UUID.randomUUID());
    }

    public static OperationId of(UUID value) {
        return new OperationId(value);
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OperationId that = (OperationId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
