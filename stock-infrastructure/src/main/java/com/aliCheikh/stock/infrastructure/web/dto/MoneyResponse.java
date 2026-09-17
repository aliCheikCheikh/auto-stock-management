package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

/** Monetary amount in the API representation. */
public record MoneyResponse(String amount,
                            String currency) {

    /**
     * Shared monetary conversion for web responses. Uses plain decimal notation; returns null when
     * the amount is absent.
     */
    public static MoneyResponse from(Money money) {
        if (money == null) {
            return null;
        }

        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode());
    }
}
