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
 *
 * <p>La règle est unique, mais elle se lit de deux façons selon que la dette est vivante ou
 * éteinte : « ouverte depuis 40 jours, donc en retard » pour l'une, « réglée en 40 jours, donc
 * hors délai » pour l'autre. C'est le même calcul et le même seuil ; seul l'instant de référence
 * change — aujourd'hui d'un côté, le dernier encaissement de l'autre. Les deux lectures portent
 * chacune leur nom, parce que le patron ne les confond pas.</p>
 */
public final class CreditPolicy {

    /** Délai au-delà duquel une créance est jugée en retard. */
    public static final int OVERDUE_AFTER_DAYS = 30;

    private CreditPolicy() {
    }

    /** Nombre de jours pendant lesquels la créance est restée ouverte, à ce jour. */
    public static long daysOutstanding(LocalDateTime saleDate, LocalDateTime now) {
        return daysBetween(saleDate, now);
    }

    /** {@code true} si la créance, toujours ouverte, dépasse le délai toléré. */
    public static boolean isOverdue(LocalDateTime saleDate, LocalDateTime now) {
        return exceedsTolerance(saleDate, now);
    }

    /** Nombre de jours qu'il aura fallu pour éteindre la créance. */
    public static long daysToSettle(LocalDateTime saleDate, LocalDateTime settledAt) {
        return daysBetween(saleDate, settledAt);
    }

    /**
     * {@code true} si la créance a été éteinte au-delà du délai toléré.
     *
     * <p>L'information survit au règlement : un client qui solde systématiquement à soixante jours
     * reste un client à qui l'on hésite à faire crédit.</p>
     */
    public static boolean wasSettledLate(LocalDateTime saleDate, LocalDateTime settledAt) {
        return exceedsTolerance(saleDate, settledAt);
    }

    /**
     * Un horodatage antérieur à la vente donnerait une durée négative, dénuée de sens ici. Plutôt
     * que d'échouer sur une donnée que seule une horloge déréglée peut produire, la durée est
     * ramenée à zéro : l'écran reste lisible.
     */
    private static long daysBetween(LocalDateTime saleDate, LocalDateTime reference) {
        Objects.requireNonNull(saleDate, "saleDate cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");

        return Math.max(0, Duration.between(saleDate, reference).toDays());
    }

    private static boolean exceedsTolerance(LocalDateTime saleDate, LocalDateTime reference) {
        return daysBetween(saleDate, reference) > OVERDUE_AFTER_DAYS;
    }
}
