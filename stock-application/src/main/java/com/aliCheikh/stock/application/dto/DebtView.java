package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Flat debt read model with customer details. Age, overdue status and settlement date are computed
 * by the use case. {@code lastPaymentAt} is null if no payment exists.
 */
public record DebtView(UUID saleId,
                       LocalDateTime occurredAt,
                       UUID customerId,
                       String customerGivenName,
                       String customerFatherName,
                       String customerPhoneNumber,
                       Money totalAmount,
                       Money amountPaid,
                       Money amountDue,
                       LocalDateTime lastPaymentAt) {
}
