package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.RecordPaymentCommand;
import com.aliCheikh.stock.application.dto.RecordPaymentResult;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.sale.PaymentExceedsAmountDueException;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineInput;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RecordPaymentUseCaseTest {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T10:00:00Z"), ZoneId.of("UTC"));

    private SaleRepository saleRepository;
    private RecordPaymentUseCase useCase;

    private static Money xaf(long amount) {
        return Money.create(BigDecimal.valueOf(amount), XAF);
    }

    /** Vente de 50 000 avec 20 000 déjà versés : il reste 30 000 dus. */
    private static Sale creditSale() {
        return Sale.create(
                UserId.generate(),
                List.of(new SaleLineInput(ProductId.generate(), 2, xaf(25_000))),
                CustomerId.generate(),
                xaf(20_000));
    }

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override
            public <T> T execute(Supplier<T> work) {
                return work.get();
            }
        };
        useCase = new RecordPaymentUseCase(saleRepository, transactionRunner, FIXED_CLOCK);
    }

    @Test
    public void should_record_the_payment_and_return_the_remaining_balance() {
        Sale sale = creditSale();
        given(saleRepository.findByIdForUpdate(any())).willReturn(Optional.of(sale));

        RecordPaymentResult result = useCase.record(
                new RecordPaymentCommand(sale.getSaleId(), xaf(12_000), UserId.generate()));

        assertThat(result.amountPaid()).isEqualTo(xaf(12_000));
        assertThat(result.totalCollected()).isEqualTo(xaf(32_000));
        assertThat(result.amountDue()).isEqualTo(xaf(18_000));
        assertThat(result.settled()).isFalse();
        verify(saleRepository).save(sale);
    }

    @Test
    public void should_report_a_settled_sale_once_the_balance_reaches_zero() {
        Sale sale = creditSale();
        given(saleRepository.findByIdForUpdate(any())).willReturn(Optional.of(sale));

        RecordPaymentResult result = useCase.record(
                new RecordPaymentCommand(sale.getSaleId(), xaf(30_000), UserId.generate()));

        assertThat(result.amountDue()).isEqualTo(xaf(0));
        assertThat(result.settled()).isTrue();
    }

    @Test
    public void should_load_the_sale_with_exclusive_access() {
        Sale sale = creditSale();
        given(saleRepository.findByIdForUpdate(any())).willReturn(Optional.of(sale));

        useCase.record(new RecordPaymentCommand(sale.getSaleId(), xaf(1_000), UserId.generate()));

        // Une lecture ordinaire laisserait deux règlements simultanés lire le même solde.
        verify(saleRepository).findByIdForUpdate(sale.getSaleId());
    }

    @Test
    public void should_fail_when_the_sale_does_not_exist() {
        SaleId unknown = SaleId.generate();
        given(saleRepository.findByIdForUpdate(unknown)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.record(
                new RecordPaymentCommand(unknown, xaf(1_000), UserId.generate())))
                .isInstanceOf(SaleNotFoundException.class);
    }

    @Test
    public void should_not_save_when_the_payment_is_rejected() {
        Sale sale = creditSale();
        given(saleRepository.findByIdForUpdate(any())).willReturn(Optional.of(sale));

        assertThatThrownBy(() -> useCase.record(
                new RecordPaymentCommand(sale.getSaleId(), xaf(40_000), UserId.generate())))
                .isInstanceOf(PaymentExceedsAmountDueException.class);

        verify(saleRepository, never()).save(any());
    }
}
