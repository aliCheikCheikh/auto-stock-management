package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Debt enriched with credit policy. {@code settledAt} is null while outstanding. {@code
 * daysOutstanding} stops at settlement; {@code overdue} uses that same duration for both open and
 * settled debts.
 */
public record DebtSummary(UUID saleId,
                          LocalDateTime occurredAt,
                          UUID customerId,
                          String customerGivenName,
                          String customerFatherName,
                          String customerPhoneNumber,
                          Money totalAmount,
                          Money amountPaid,
                          Money amountDue,
                          boolean settled,
                          LocalDateTime settledAt,
                          long daysOutstanding,
                          boolean overdue) {
}
