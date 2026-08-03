package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.RecordPaymentResult;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Encaissement enregistré, avec l'état de la dette après opération.
 *
 * <p>Le solde est renvoyé pour que le vendeur l'annonce au client sans que l'interface ait à le
 * recalculer : une seule source de vérité.</p>
 */
public record PaymentResponse(UUID saleId,
                              MoneyResponse amountPaid,
                              LocalDateTime receivedAt,
                              MoneyResponse totalAmount,
                              MoneyResponse totalCollected,
                              MoneyResponse amountDue,
                              boolean settled) {

    public static PaymentResponse from(RecordPaymentResult result) {
        return new PaymentResponse(
                result.saleId().getValue(),
                MoneyResponse.from(result.amountPaid()),
                result.receivedAt(),
                MoneyResponse.from(result.totalAmount()),
                MoneyResponse.from(result.totalCollected()),
                MoneyResponse.from(result.amountDue()),
                result.settled());
    }
}
