package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection d'interface Spring Data pour les créances.
 *
 * <p>Chaque colonne est lue par son nom via un accesseur typé. Une projection positionnelle
 * ({@code Object[]}) obligerait à connaître l'ordre du SELECT et casserait silencieusement à la
 * moindre réorganisation de la requête.</p>
 */
public interface DebtProjection {

    UUID getSaleId();

    LocalDateTime getOccurredAt();

    UUID getCustomerId();

    String getCustomerGivenName();

    String getCustomerFatherName();

    String getCustomerPhoneNumber();

    BigDecimal getTotalAmount();

    String getCurrency();

    BigDecimal getAmountPaid();

    /** Dernier encaissement, {@code null} si le client n'a jamais rien versé. */
    LocalDateTime getLastPaymentAt();
}
