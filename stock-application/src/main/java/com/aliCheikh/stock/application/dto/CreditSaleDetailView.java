package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Credit sale details, including line items and payments. The balance and settlement status are
 * derived from the total and payment ledger.
 */
public record CreditSaleDetailView(UUID saleId,
                                   LocalDateTime occurredAt,
                                   UUID sellerId,
                                   String sellerName,
                                   UUID customerId,
                                   String customerGivenName,
                                   String customerFatherName,
                                   String customerPhoneNumber,
                                   List<CreditSaleLineView> lines,
                                   Money totalAmount,
                                   Money amountPaid,
                                   Money amountDue,
                                   boolean settled,
                                   List<CreditSalePaymentView> payments) {
}
