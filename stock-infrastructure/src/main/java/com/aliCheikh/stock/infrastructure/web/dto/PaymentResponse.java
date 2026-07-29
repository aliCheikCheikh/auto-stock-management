package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.RecordPaymentResult;
import com.aliCheikh.stock.domain.model.shared.Money;

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
                toMoney(result.amountPaid()),
                result.receivedAt(),
                toMoney(result.totalAmount()),
                toMoney(result.totalCollected()),
                toMoney(result.amountDue()),
                result.settled());
    }

    private static MoneyResponse toMoney(Money money) {
        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode());
    }
}
