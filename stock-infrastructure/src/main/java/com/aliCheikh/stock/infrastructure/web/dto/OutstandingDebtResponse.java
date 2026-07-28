package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.OutstandingDebtView;

import java.time.LocalDateTime;
import java.util.UUID;

/** Une créance telle qu'affichée dans l'écran « qui me doit de l'argent ». */
public record OutstandingDebtResponse(UUID saleId,
                                      LocalDateTime occurredAt,
                                      UUID customerId,
                                      String customerGivenName,
                                      String customerFatherName,
                                      String customerPhoneNumber,
                                      MoneyResponse totalAmount,
                                      MoneyResponse amountPaid,
                                      MoneyResponse amountDue) {

    public static OutstandingDebtResponse from(OutstandingDebtView view) {
        return new OutstandingDebtResponse(
                view.saleId(),
                view.occurredAt(),
                view.customerId(),
                view.customerGivenName(),
                view.customerFatherName(),
                view.customerPhoneNumber(),
                toMoney(view.totalAmount()),
                toMoney(view.amountPaid()),
                toMoney(view.amountDue())
        );
    }

    private static MoneyResponse toMoney(com.aliCheikh.stock.domain.model.shared.Money money) {
        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode()
        );
    }
}
