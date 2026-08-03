package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un encaissement, du plus ancien au plus récent.
 *
 * <p>L'acompte versé le jour de la vente n'a rien de particulier : c'est le premier paiement du
 * ledger. Le patron lit ainsi une seule histoire — ce qui a été versé, quand, et par qui reçu.</p>
 *
 * @param receivedByName {@code null} si le compte de l'encaisseur a disparu
 */
public record CreditSalePaymentView(UUID paymentId,
                                    Money amount,
                                    LocalDateTime receivedAt,
                                    UUID receivedById,
                                    String receivedByName) {
}
