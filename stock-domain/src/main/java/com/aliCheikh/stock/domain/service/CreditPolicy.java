package com.aliCheikh.stock.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Politique de crédit du magasin.
 *
 * <p>Au-delà d'un certain délai, une créance est considérée en retard et justifie une relance.
 * Ce seuil est une <b>décision métier</b> : il appartient au domaine, pas à un écran. Le placer
 * dans l'interface conduirait à ce que le front, un export et une future notification appliquent
 * chacun leur propre valeur.</p>
 */
public final class CreditPolicy {

    /** Délai au-delà duquel une créance est jugée en retard. */
    public static final int OVERDUE_AFTER_DAYS = 30;

    private CreditPolicy() {
    }

    /** Nombre de jours écoulés depuis la vente. */
    public static long daysOutstanding(LocalDateTime saleDate, LocalDateTime now) {
        Objects.requireNonNull(saleDate, "saleDate cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        return Math.max(0, Duration.between(saleDate, now).toDays());
    }

    /** {@code true} si la créance dépasse le délai toléré. */
    public static boolean isOverdue(LocalDateTime saleDate, LocalDateTime now) {
        return daysOutstanding(saleDate, now) > OVERDUE_AFTER_DAYS;
    }
}
