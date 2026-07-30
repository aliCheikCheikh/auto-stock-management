package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.application.dto.CreditSaleLineView;
import com.aliCheikh.stock.application.dto.CreditSalePaymentView;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Le détail d'une créance : ce qui a été vendu, à qui, par qui, et ce qui reste dû.
 *
 * <p>{@code amountDue} et {@code settled} sont fournis pour que le front n'ait jamais à refaire un
 * calcul de montant : la seule source de vérité est le serveur.</p>
 */
public record CreditSaleDetailResponse(UUID saleId,
                                       LocalDateTime occurredAt,
                                       UUID sellerId,
                                       String sellerName,
                                       UUID customerId,
                                       String customerGivenName,
                                       String customerFatherName,
                                       String customerPhoneNumber,
                                       List<CreditSaleLineResponse> lines,
                                       MoneyResponse totalAmount,
                                       MoneyResponse amountPaid,
                                       MoneyResponse amountDue,
                                       boolean settled,
                                       List<CreditSalePaymentResponse> payments) {

    public static CreditSaleDetailResponse from(CreditSaleDetailView view) {
        return new CreditSaleDetailResponse(
                view.saleId(),
                view.occurredAt(),
                view.sellerId(),
                view.sellerName(),
                view.customerId(),
                view.customerGivenName(),
                view.customerFatherName(),
                view.customerPhoneNumber(),
                view.lines().stream().map(CreditSaleDetailResponse::toLine).toList(),
                toMoney(view.totalAmount()),
                toMoney(view.amountPaid()),
                toMoney(view.amountDue()),
                view.settled(),
                view.payments().stream().map(CreditSaleDetailResponse::toPayment).toList());
    }

    private static CreditSaleLineResponse toLine(CreditSaleLineView line) {
        return new CreditSaleLineResponse(
                line.productId(),
                line.productName(),
                line.productReference(),
                line.quantity(),
                toMoney(line.unitPrice()),
                toMoney(line.lineTotal()));
    }

    private static CreditSalePaymentResponse toPayment(CreditSalePaymentView payment) {
        return new CreditSalePaymentResponse(
                payment.paymentId(),
                toMoney(payment.amount()),
                payment.receivedAt(),
                payment.receivedById(),
                payment.receivedByName());
    }

    private static MoneyResponse toMoney(Money money) {
        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode());
    }
}
