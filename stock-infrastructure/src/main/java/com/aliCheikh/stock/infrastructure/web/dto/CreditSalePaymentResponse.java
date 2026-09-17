package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Payment response. {@code receivedByName} is null if the receiver account no longer exists. */
public record CreditSalePaymentResponse(UUID paymentId,
                                        MoneyResponse amount,
                                        LocalDateTime receivedAt,
                                        UUID receivedById,
                                        String receivedByName) {
}
