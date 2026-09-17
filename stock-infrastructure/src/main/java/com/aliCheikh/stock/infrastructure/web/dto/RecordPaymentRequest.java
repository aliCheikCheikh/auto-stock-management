package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Payment request. Input validation checks presence and positivity; the domain checks the
 * outstanding balance.
 */
public record RecordPaymentRequest(@NotNull @Positive BigDecimal amount) {
}
