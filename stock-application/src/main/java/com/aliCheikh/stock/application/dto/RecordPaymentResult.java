package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;

/** Payment result with the remaining balance computed by the backend. */
public record RecordPaymentResult(SaleId saleId,
                                  Money amountPaid,
                                  LocalDateTime receivedAt,
                                  Money totalAmount,
                                  Money totalCollected,
                                  Money amountDue,
                                  boolean settled) {
}
