package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.exception.money.CurrencyMismatchException;
import com.aliCheikh.stock.domain.exception.sale.PaymentExceedsAmountDueException;
import com.aliCheikh.stock.domain.exception.sale.SaleAlreadySettledException;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Repayments on a credit sale. */
public class SalePaymentTest {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final LocalDateTime LATER = LocalDateTime.of(2026, 8, 15, 10, 0);

    private static Money xaf(long amount) {
        return Money.create(BigDecimal.valueOf(amount), XAF);
    }

    /** Une vente de 50 000 FCFA. */
    private static List<SaleLineInput> linesTotalling50000() {
        return List.of(new SaleLineInput(ProductId.generate(), 2, xaf(25_000)));
    }

    private static Sale creditSaleWithDownPayment(long downPayment) {
        return Sale.create(UserId.generate(), linesTotalling50000(), CustomerId.generate(), xaf(downPayment));
    }

    @Test
    public void the_down_payment_is_recorded_as_the_first_payment() {
        Sale sale = creditSaleWithDownPayment(20_000);

        assertThat(sale.getPayments()).hasSize(1);
        assertThat(sale.getPayments().get(0).getAmount()).isEqualTo(xaf(20_000));
        assertThat(sale.getAmountPaid()).isEqualTo(xaf(20_000));
        assertThat(sale.getAmountDue()).isEqualTo(xaf(30_000));
    }

    @Test
    public void a_sale_without_down_payment_records_no_payment_at_all() {
        // A zero initial payment creates no ledger entry.
        Sale sale = creditSaleWithDownPayment(0);

        assertThat(sale.getPayments()).isEmpty();
        assertThat(sale.getAmountPaid()).isEqualTo(xaf(0));
        assertThat(sale.getAmountDue()).isEqualTo(xaf(50_000));
    }

    @Test
    public void a_repayment_reduces_the_outstanding_balance() {
        Sale sale = creditSaleWithDownPayment(20_000);
        UserId cashier = UserId.generate();

        sale.recordPayment(xaf(12_000), cashier, LATER);

        assertThat(sale.getAmountPaid()).isEqualTo(xaf(32_000));
        assertThat(sale.getAmountDue()).isEqualTo(xaf(18_000));
        assertThat(sale.isOnCredit()).isTrue();
        assertThat(sale.getPayments()).hasSize(2);
    }

    @Test
    public void repayments_accumulate_until_the_sale_is_settled() {
        Sale sale = creditSaleWithDownPayment(0);
        UserId cashier = UserId.generate();

        sale.recordPayment(xaf(20_000), cashier, LATER);
        sale.recordPayment(xaf(30_000), cashier, LATER.plusDays(3));

        assertThat(sale.getAmountDue()).isEqualTo(xaf(0));
        assertThat(sale.isOnCredit()).isFalse();
        assertThat(sale.getPayments()).hasSize(2);
    }

    @Test
    public void a_payment_keeps_track_of_who_collected_it_and_when() {
        Sale sale = creditSaleWithDownPayment(0);
        UserId cashier = UserId.generate();

        Payment payment = sale.recordPayment(xaf(5_000), cashier, LATER);

        assertThat(payment.getReceivedBy()).isEqualTo(cashier);
        assertThat(payment.getReceivedAt()).isEqualTo(LATER);
    }

    // --- Invariants ---

    @Test
    public void should_reject_a_payment_larger_than_the_outstanding_balance() {
        Sale sale = creditSaleWithDownPayment(20_000);

        assertThatThrownBy(() -> sale.recordPayment(xaf(30_001), UserId.generate(), LATER))
                .isInstanceOf(PaymentExceedsAmountDueException.class);

        // Rejected payments must leave the aggregate unchanged.
        assertThat(sale.getAmountDue()).isEqualTo(xaf(30_000));
        assertThat(sale.getPayments()).hasSize(1);
    }

    @Test
    public void should_accept_a_payment_that_settles_the_balance_exactly() {
        Sale sale = creditSaleWithDownPayment(20_000);

        sale.recordPayment(xaf(30_000), UserId.generate(), LATER);

        assertThat(sale.getAmountDue()).isEqualTo(xaf(0));
        assertThat(sale.isOnCredit()).isFalse();
    }

    @Test
    public void should_reject_a_payment_on_a_settled_sale() {
        Sale cashSale = Sale.create(UserId.generate(), linesTotalling50000());

        assertThatThrownBy(() -> cashSale.recordPayment(xaf(1_000), UserId.generate(), LATER))
                .isInstanceOf(SaleAlreadySettledException.class);
    }

    @Test
    public void should_reject_a_payment_of_zero_or_less() {
        Sale sale = creditSaleWithDownPayment(0);

        assertThatThrownBy(() -> sale.recordPayment(xaf(0), UserId.generate(), LATER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sale.recordPayment(xaf(-100), UserId.generate(), LATER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void should_reject_a_payment_in_another_currency() {
        Sale sale = creditSaleWithDownPayment(0);
        Money inEuros = Money.create(BigDecimal.valueOf(50), Currency.getInstance("EUR"));

        assertThatThrownBy(() -> sale.recordPayment(inEuros, UserId.generate(), LATER))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    public void the_payments_list_cannot_be_modified_from_outside() {
        Sale sale = creditSaleWithDownPayment(20_000);

        assertThatThrownBy(() -> sale.getPayments().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
