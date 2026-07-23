package com.aliCheikh.stock.domain.model.shared;

import com.aliCheikh.stock.domain.exception.money.CurrencyMismatchException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MoneyTest {

    private static final Currency XAF = Currency.getInstance("XAF"); // Franc CFA (Tchad)
    private static final Currency EUR = Currency.getInstance("EUR");

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }

    @Test
    public void should_subtract_two_amounts_of_same_currency() {
        // GIVEN un total de 50 000 et un acompte de 20 000
        // WHEN on calcule le reste dû
        Money remaining = xaf("50000").subtract(xaf("20000"));

        // THEN il reste 30 000
        assertThat(remaining).isEqualTo(xaf("30000"));
    }

    @Test
    public void should_return_negative_money_when_subtracting_a_bigger_amount() {
        // GIVEN on paie 50 000 pour un total de 20 000 (cas que Sale interdira plus tard)
        // WHEN on soustrait
        Money result = xaf("20000").subtract(xaf("50000"));

        // THEN le résultat est négatif : Money reste neutre, c'est la règle métier qui tranchera
        assertThat(result.isNegative()).isTrue();
        assertThat(result).isEqualTo(xaf("-30000"));
    }

    @Test
    public void should_reject_subtraction_between_different_currencies() {
        assertThatThrownBy(() -> xaf("50000").subtract(Money.create(new BigDecimal("10"), EUR)))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    public void should_reject_null_in_subtraction() {
        assertThatNullPointerException()
                .isThrownBy(() -> xaf("50000").subtract(null));
    }

    @Test
    public void is_negative_should_be_true_only_below_zero() {
        assertThat(xaf("-1").isNegative()).isTrue();
        assertThat(xaf("0").isNegative()).isFalse();
        assertThat(xaf("1").isNegative()).isFalse();
    }
}
