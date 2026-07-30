package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.application.port.CreditSaleDetailQueryPort;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class GetCreditSaleDetailUseCaseTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    private CreditSaleDetailQueryPort creditSaleDetailQueryPort;
    private GetCreditSaleDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        creditSaleDetailQueryPort = Mockito.mock(CreditSaleDetailQueryPort.class);
        useCase = new GetCreditSaleDetailUseCase(creditSaleDetailQueryPort);
    }

    @Test
    void should_return_the_detail_of_an_existing_credit_sale() {
        SaleId saleId = SaleId.generate();
        CreditSaleDetailView expected = detailOf(saleId);

        given(creditSaleDetailQueryPort.findCreditSaleDetail(saleId.getValue()))
                .willReturn(Optional.of(expected));

        assertThat(useCase.detailOf(saleId)).isEqualTo(expected);
    }

    /**
     * Une vente au comptant n'est pas une créance. La signaler comme introuvable plutôt que comme
     * refusée évite de renseigner l'appelant sur l'existence d'une vente qui ne le regarde pas.
     */
    @Test
    void should_reject_a_sale_which_is_not_a_debt() {
        SaleId saleId = SaleId.generate();

        given(creditSaleDetailQueryPort.findCreditSaleDetail(saleId.getValue()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.detailOf(saleId))
                .isInstanceOf(SaleNotFoundException.class);
    }

    private static CreditSaleDetailView detailOf(SaleId saleId) {
        return new CreditSaleDetailView(
                saleId.getValue(),
                LocalDateTime.of(2026, 7, 20, 10, 30),
                UUID.randomUUID(),
                "Ahmat",
                UUID.randomUUID(),
                "Moussa",
                "Youssouf",
                "+23566123456",
                List.of(),
                xaf("200000"),
                xaf("100000"),
                xaf("100000"),
                false,
                List.of());
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
