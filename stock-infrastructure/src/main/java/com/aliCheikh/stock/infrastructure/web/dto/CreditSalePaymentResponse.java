package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un encaissement de l'échéancier.
 *
 * @param receivedByName {@code null} si le compte de l'encaisseur a disparu ; le front affiche
 *                       alors un repli neutre plutôt qu'un vide
 */
public record CreditSalePaymentResponse(UUID paymentId,
                                        MoneyResponse amount,
                                        LocalDateTime receivedAt,
                                        UUID receivedById,
                                        String receivedByName) {
}
