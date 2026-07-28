package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.OutstandingDebtSummary;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une créance telle qu'affichée dans l'écran « qui me doit de l'argent ».
 *
 * <p>{@code daysOutstanding} et {@code overdue} viennent de la politique de crédit du domaine :
 * l'interface les affiche, elle ne les recalcule pas.</p>
 */
public record OutstandingDebtResponse(UUID saleId,
                                      LocalDateTime occurredAt,
                                      UUID customerId,
                                      String customerGivenName,
                                      String customerFatherName,
                                      String customerPhoneNumber,
                                      MoneyResponse totalAmount,
                                      MoneyResponse amountPaid,
                                      MoneyResponse amountDue,
                                      long daysOutstanding,
                                      boolean overdue) {

    public static OutstandingDebtResponse from(OutstandingDebtSummary summary) {
        return new OutstandingDebtResponse(
                summary.saleId(),
                summary.occurredAt(),
                summary.customerId(),
                summary.customerGivenName(),
                summary.customerFatherName(),
                summary.customerPhoneNumber(),
                toMoney(summary.totalAmount()),
                toMoney(summary.amountPaid()),
                toMoney(summary.amountDue()),
                summary.daysOutstanding(),
                summary.overdue());
    }

    private static MoneyResponse toMoney(Money money) {
        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode());
    }
}
