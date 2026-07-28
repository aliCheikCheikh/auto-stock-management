package com.aliCheikh.stock.domain.model.shared;

import com.aliCheikh.stock.domain.exception.money.CurrencyMismatchException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
    }


    public static Money create(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other money cannot be null");

        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }

        BigDecimal newAmount = this.amount.add(other.amount);
        return new Money(newAmount, currency);
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other money cannot be null");
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(this.currency, other.currency);
        }
        BigDecimal newAmount = this.amount.subtract(other.amount);
        return new Money(newAmount, currency);
    }

    public Money multiply(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }

        BigDecimal newAmount = this.amount.multiply(BigDecimal.valueOf(quantity));

        return new Money(newAmount, currency);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        // Attention : Pour BigDecimal, on utilise compareTo plutôt que equals
        // pour que 10.0 soit égal à 10.00
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        // Forme lisible dans les logs et les messages d'exception : "50000 XAF".
        // toPlainString() évite la notation scientifique (5E+4) sur les gros montants.
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }


}