package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entry, ordered oldest first. The initial payment is part of the same ledger. {@code
 * receivedByName} is null if the receiver account no longer exists.
 */
public record CreditSalePaymentView(UUID paymentId,
                                    Money amount,
                                    LocalDateTime receivedAt,
                                    UUID receivedById,
                                    String receivedByName) {
}
