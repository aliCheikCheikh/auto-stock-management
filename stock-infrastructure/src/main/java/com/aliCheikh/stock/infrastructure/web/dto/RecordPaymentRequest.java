package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Encaissement d'un remboursement.
 *
 * <p>La validation ne fait ici que rejeter l'absurde (montant manquant ou négatif) ; savoir si le
 * montant est recevable au regard du solde est une règle métier, portée par le domaine.</p>
 */
public record RecordPaymentRequest(@NotNull @Positive BigDecimal amount) {
}
