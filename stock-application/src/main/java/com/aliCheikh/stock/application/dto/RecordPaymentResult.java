package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;

/**
 * Résultat d'un encaissement.
 *
 * <p>Le solde restant est renvoyé pour que le vendeur l'annonce au client sans avoir à le
 * recalculer, et pour qu'il soit affiché depuis la seule source de vérité.</p>
 */
public record RecordPaymentResult(SaleId saleId,
                                  Money amountPaid,
                                  LocalDateTime receivedAt,
                                  Money totalAmount,
                                  Money totalCollected,
                                  Money amountDue,
                                  boolean settled) {
}
