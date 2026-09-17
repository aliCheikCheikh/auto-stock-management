package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.RecordPaymentResult;

import java.time.LocalDateTime;
import java.util.UUID;

/** Recorded payment with the server-computed remaining balance. */
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
