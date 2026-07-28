package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.exception.money.CurrencyMismatchException;
import com.aliCheikh.stock.domain.exception.sale.CreditSaleRequiresCustomerException;
import com.aliCheikh.stock.domain.exception.sale.InvalidSaleException;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SaleTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    private static Money eur(long amount) {
        return Money.create(BigDecimal.valueOf(amount), EUR);
    }

    /** Deux lignes totalisant 60 EUR (2 filtres à 15, 1 bidon d'huile à 30). */
    private static List<SaleLineInput> linesTotalling60() {
        return List.of(
                new SaleLineInput(ProductId.generate(), 2, eur(15)),
                new SaleLineInput(ProductId.generate(), 1, eur(30))
        );
    }

    @Test
    public void should_throw_exception_when_creating_sale_without_lines() {
        // GIVEN
        UserId sellerId = UserId.generate();
        List<SaleLineInput> emptyLines = new ArrayList<>();

        // WHEN & THEN
        assertThatThrownBy(() -> Sale.create(sellerId, emptyLines))
                .isInstanceOf(InvalidSaleException.class)
                .extracting(ex -> (InvalidSaleException) ex)
                .satisfies(ex -> {
                    // Verifying the exact message thrown by our Sale aggregate
                    assertThat(ex.getMessage()).contains("at least one line item");
                });
    }

    @Test
    public void should_calculate_total_amount_correctly_when_creating_sale() {
        // GIVEN: A seller and two items (2 filters at 15 EUR, 1 oil can at 30 EUR)
        UserId sellerId = UserId.generate();

        // WHEN: Creating the sale
        Sale sale = Sale.create(sellerId, linesTotalling60());

        // THEN: The total must be exactly 60 EUR
        assertThat(sale.getTotalAmount()).isEqualTo(eur(60));
        assertThat(sale.getSoldBy()).isEqualTo(sellerId);
        assertThat(sale.getLines()).hasSize(2);
        assertThat(sale.getOccurredAt()).isNotNull();
        assertThat(sale.getSaleId()).isNotNull();
    }

    // --- Vente au comptant et ventes à crédit ---

    @Test
    public void cash_sale_is_paid_in_full_and_needs_no_customer() {
        UserId sellerId = UserId.generate();

        Sale sale = Sale.create(sellerId, linesTotalling60());

        assertThat(sale.getAmountPaid()).isEqualTo(eur(60));
        assertThat(sale.getAmountDue()).isEqualTo(eur(0));
        assertThat(sale.isOnCredit()).isFalse();
        assertThat(sale.getCustomerId()).isEmpty();
    }

    @Test
    public void credit_sale_with_partial_down_payment_keeps_the_balance_due() {
        UserId sellerId = UserId.generate();
        CustomerId customerId = CustomerId.generate();

        Sale sale = Sale.create(sellerId, linesTotalling60(), customerId, eur(20));

        assertThat(sale.getAmountPaid()).isEqualTo(eur(20));
        assertThat(sale.getAmountDue()).isEqualTo(eur(40));
        assertThat(sale.isOnCredit()).isTrue();
        assertThat(sale.getCustomerId()).contains(customerId);
    }

    @Test
    public void credit_sale_without_down_payment_owes_the_whole_total() {
        // Le client de confiance repart sans rien payer : toute la vente est une créance.
        UserId sellerId = UserId.generate();
        CustomerId customerId = CustomerId.generate();

        Sale sale = Sale.create(sellerId, linesTotalling60(), customerId, eur(0));

        assertThat(sale.getAmountDue()).isEqualTo(eur(60));
        assertThat(sale.isOnCredit()).isTrue();
        assertThat(sale.getCustomerId()).contains(customerId);
    }

    @Test
    public void sale_paid_in_full_with_an_explicit_amount_needs_no_customer() {
        UserId sellerId = UserId.generate();

        Sale sale = Sale.create(sellerId, linesTotalling60(), null, eur(60));

        assertThat(sale.getAmountDue()).isEqualTo(eur(0));
        assertThat(sale.isOnCredit()).isFalse();
    }

    // --- Invariants refusés ---

    @Test
    public void should_reject_a_negative_down_payment() {
        UserId sellerId = UserId.generate();

        assertThatThrownBy(() ->
                Sale.create(sellerId, linesTotalling60(), CustomerId.generate(), eur(-10)))
                .isInstanceOf(InvalidSaleException.class)
                .hasMessageContaining("cannot be negative");
    }

    @Test
    public void should_reject_a_down_payment_greater_than_the_total() {
        UserId sellerId = UserId.generate();

        assertThatThrownBy(() ->
                Sale.create(sellerId, linesTotalling60(), CustomerId.generate(), eur(100)))
                .isInstanceOf(InvalidSaleException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    public void should_reject_a_sale_leaving_a_balance_without_a_customer() {
        UserId sellerId = UserId.generate();

        // Reste dû = 40 EUR, mais aucun client à qui réclamer la créance.
        assertThatThrownBy(() ->
                Sale.create(sellerId, linesTotalling60(), null, eur(20)))
                .isInstanceOf(CreditSaleRequiresCustomerException.class)
                .extracting(ex -> ((CreditSaleRequiresCustomerException) ex).getAmountDue())
                .isEqualTo(eur(40));
    }

    @Test
    public void should_reject_a_down_payment_in_another_currency() {
        UserId sellerId = UserId.generate();
        Money downPaymentInXaf = Money.create(BigDecimal.valueOf(20), Currency.getInstance("XAF"));

        assertThatThrownBy(() ->
                Sale.create(sellerId, linesTotalling60(), CustomerId.generate(), downPaymentInXaf))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    // --- Reconstruction depuis la persistance ---

    @Test
    public void rehydrated_cash_sale_owes_nothing() {
        UserId sellerId = UserId.generate();
        Sale original = Sale.create(sellerId, linesTotalling60());

        Sale rehydrated = Sale.rehydrate(
                original.getSaleId(),
                sellerId,
                original.getOccurredAt(),
                original.getTotalAmount(),
                original.getLines());

        assertThat(rehydrated.getAmountDue()).isEqualTo(eur(0));
        assertThat(rehydrated.getCustomerId()).isEmpty();
    }

    @Test
    public void rehydrated_credit_sale_keeps_its_balance_and_customer() {
        UserId sellerId = UserId.generate();
        CustomerId customerId = CustomerId.generate();
        Sale original = Sale.create(sellerId, linesTotalling60(), customerId, eur(20));

        Sale rehydrated = Sale.rehydrate(
                original.getSaleId(),
                sellerId,
                original.getOccurredAt(),
                original.getTotalAmount(),
                original.getLines(),
                customerId,
                original.getAmountPaid());

        assertThat(rehydrated.getAmountDue()).isEqualTo(eur(40));
        assertThat(rehydrated.getCustomerId()).contains(customerId);
    }
}
