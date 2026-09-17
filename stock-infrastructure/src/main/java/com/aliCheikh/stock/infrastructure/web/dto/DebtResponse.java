package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.DebtSummary;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Debt response with server-computed age and overdue status. Age stops at settlement; {@code
 * settledAt} is null while outstanding. Overdue means still overdue or settled late, depending on
 * status.
 */
public record DebtResponse(UUID saleId,
                           LocalDateTime occurredAt,
                           UUID customerId,
                           String customerGivenName,
                           String customerFatherName,
                           String customerPhoneNumber,
                           MoneyResponse totalAmount,
                           MoneyResponse amountPaid,
                           MoneyResponse amountDue,
                           boolean settled,
                           LocalDateTime settledAt,
                           long daysOutstanding,
                           boolean overdue) {

    public static DebtResponse from(DebtSummary summary) {
        return new DebtResponse(
                summary.saleId(),
                summary.occurredAt(),
                summary.customerId(),
                summary.customerGivenName(),
                summary.customerFatherName(),
                summary.customerPhoneNumber(),
                MoneyResponse.from(summary.totalAmount()),
                MoneyResponse.from(summary.amountPaid()),
                MoneyResponse.from(summary.amountDue()),
                summary.settled(),
                summary.settledAt(),
                summary.daysOutstanding(),
                summary.overdue());
    }
}
