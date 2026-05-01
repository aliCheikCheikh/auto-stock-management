package com.aliCheikh.stock.domain.exception.money;

import com.aliCheikh.stock.domain.exception.DomainException;

import java.util.Currency;

public class CurrencyMismatchException extends DomainException {

    private final Currency expectedCurrency;
    private final Currency actualCurrency;

    public CurrencyMismatchException(Currency expectedCurrency, Currency actualCurrency) {
        // Message ultra-précis pour les logs backend
        super(String.format("Cannot perform operation on different currencies: expected %s, but got %s",
                expectedCurrency.getCurrencyCode(), actualCurrency.getCurrencyCode()));
        this.expectedCurrency = expectedCurrency;
        this.actualCurrency = actualCurrency;
    }

    public Currency getExpectedCurrency() {
        return expectedCurrency;
    }

    public Currency getActualCurrency() {
        return actualCurrency;
    }
}