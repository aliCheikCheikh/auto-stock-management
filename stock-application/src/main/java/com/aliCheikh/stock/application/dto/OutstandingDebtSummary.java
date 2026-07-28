package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une créance enrichie de son ancienneté, telle que présentée au patron.
 *
 * <p>{@code overdue} traduit la politique de crédit du domaine : l'interface se contente de
 * l'afficher, elle ne décide pas de ce qui constitue un retard.</p>
 */
public record OutstandingDebtSummary(UUID saleId,
                                     LocalDateTime occurredAt,
                                     UUID customerId,
                                     String customerGivenName,
                                     String customerFatherName,
                                     String customerPhoneNumber,
                                     Money totalAmount,
                                     Money amountPaid,
                                     Money amountDue,
                                     long daysOutstanding,
                                     boolean overdue) {
}
