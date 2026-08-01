package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.application.dto.CreditSaleLineView;
import com.aliCheikh.stock.application.dto.CreditSalePaymentView;
import com.aliCheikh.stock.application.dto.DebtStatus;
import com.aliCheikh.stock.application.dto.DebtSummary;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.usecase.GetCreditSaleDetailUseCase;
import com.aliCheikh.stock.application.usecase.ListDebtsUseCase;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DebtController.class)
@AutoConfigureMockMvc(addFilters = false)
class DebtControllerTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListDebtsUseCase listDebtsUseCase;

    @MockitoBean
    private GetCreditSaleDetailUseCase getCreditSaleDetailUseCase;

    /** Requis par IdempotencyFilter, chargé par la tranche web mais dépendant de la persistance. */
    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Test
    void should_expose_outstanding_debts_with_their_balance() throws Exception {
        UUID saleId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        given(listDebtsUseCase.execute(any())).willReturn(page(new DebtSummary(
                saleId,
                LocalDateTime.of(2026, 7, 20, 10, 30),
                customerId,
                "Ahmat",
                "Youssouf",
                "+23566123456",
                xaf("50000"),
                xaf("20000"),
                xaf("30000"),
                false,
                null,
                45,
                true)));

        mockMvc.perform(get("/api/v1/debts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].customerGivenName").value("Ahmat"))
                .andExpect(jsonPath("$.content[0].customerPhoneNumber").value("+23566123456"))
                .andExpect(jsonPath("$.content[0].amountDue.amount").value("30000"))
                .andExpect(jsonPath("$.content[0].amountDue.currency").value("XAF"))
                .andExpect(jsonPath("$.content[0].settled").value(false))
                .andExpect(jsonPath("$.content[0].settledAt").doesNotExist())
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void should_return_an_empty_page_when_nobody_owes_anything() throws Exception {
        given(listDebtsUseCase.execute(any())).willReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/debts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    /**
     * Sans paramètre, l'écran répond à la question qu'on lui posait déjà. L'historique est une
     * demande explicite : il ne doit pas surgir d'une requête sans filtre.
     */
    @Test
    void should_look_at_open_debts_when_no_status_is_asked_for() throws Exception {
        given(listDebtsUseCase.execute(any())).willReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/debts")).andExpect(status().isOk());

        assertThat(capturedQuery().status()).isEqualTo(DebtStatus.OUTSTANDING);
        assertThat(capturedQuery().customerId()).isNull();
    }

    @Test
    void should_expose_a_settled_debt_with_the_day_it_was_repaid() throws Exception {
        LocalDateTime settledAt = LocalDateTime.of(2026, 7, 28, 9, 15);

        given(listDebtsUseCase.execute(any())).willReturn(page(new DebtSummary(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 7, 20, 10, 30),
                UUID.randomUUID(),
                "Ahmat",
                "Youssouf",
                "+23566123456",
                xaf("50000"),
                xaf("50000"),
                xaf("0"),
                true,
                settledAt,
                8,
                false)));

        mockMvc.perform(get("/api/v1/debts").param("status", "SETTLED").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].settled").value(true))
                .andExpect(jsonPath("$.content[0].settledAt").value("2026-07-28T09:15:00"))
                .andExpect(jsonPath("$.content[0].amountDue.amount").value("0"))
                // Réglée en 8 jours : la créance a cessé de vieillir le jour du paiement.
                .andExpect(jsonPath("$.content[0].daysOutstanding").value(8))
                .andExpect(jsonPath("$.content[0].overdue").value(false));

        assertThat(capturedQuery().status()).isEqualTo(DebtStatus.SETTLED);
        assertThat(capturedQuery().size()).isEqualTo(5);
    }

    @Test
    void should_reject_a_status_which_does_not_exist() throws Exception {
        mockMvc.perform(get("/api/v1/debts").param("status", "PEUT_ETRE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_reject_a_page_size_beyond_the_allowed_range() throws Exception {
        mockMvc.perform(get("/api/v1/debts").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    private ListDebtsQuery capturedQuery() {
        ArgumentCaptor<ListDebtsQuery> captor = ArgumentCaptor.forClass(ListDebtsQuery.class);
        verify(listDebtsUseCase, atLeastOnce()).execute(captor.capture());

        return captor.getValue();
    }

    private static PageResult<DebtSummary> page(DebtSummary summary) {
        return new PageResult<>(List.of(summary), 0, 20, 1, 1);
    }

    @Test
    void should_expose_what_was_sold_by_whom_and_what_was_already_paid() throws Exception {
        UUID saleId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        given(getCreditSaleDetailUseCase.detailOf(SaleId.of(saleId))).willReturn(new CreditSaleDetailView(
                saleId,
                LocalDateTime.of(2026, 7, 20, 10, 30),
                sellerId,
                "Ahmat",
                UUID.randomUUID(),
                "Moussa",
                "Youssouf",
                "+23566123456",
                List.of(new CreditSaleLineView(
                        productId, "Plaquettes de frein", "REF-42", 3, xaf("40000"), xaf("120000"))),
                xaf("200000"),
                xaf("100000"),
                xaf("100000"),
                false,
                List.of(new CreditSalePaymentView(
                        UUID.randomUUID(), xaf("100000"),
                        LocalDateTime.of(2026, 7, 20, 10, 30), sellerId, "Ahmat"))));

        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId))
                .andExpect(status().isOk())
                // « Pour quels produits ? » — la question à laquelle la liste ne répondait pas.
                .andExpect(jsonPath("$.lines[0].productName").value("Plaquettes de frein"))
                .andExpect(jsonPath("$.lines[0].quantity").value(3))
                .andExpect(jsonPath("$.lines[0].lineTotal.amount").value("120000"))
                // « Vendu par qui ? »
                .andExpect(jsonPath("$.sellerName").value("Ahmat"))
                .andExpect(jsonPath("$.customerGivenName").value("Moussa"))
                // « Combien payé, combien reste-t-il ? »
                .andExpect(jsonPath("$.amountPaid.amount").value("100000"))
                .andExpect(jsonPath("$.amountDue.amount").value("100000"))
                .andExpect(jsonPath("$.settled").value(false))
                .andExpect(jsonPath("$.payments[0].amount.amount").value("100000"))
                .andExpect(jsonPath("$.payments[0].receivedByName").value("Ahmat"));
    }

    @Test
    void should_still_expose_the_detail_once_the_debt_is_settled() throws Exception {
        UUID saleId = UUID.randomUUID();

        given(getCreditSaleDetailUseCase.detailOf(SaleId.of(saleId))).willReturn(new CreditSaleDetailView(
                saleId,
                LocalDateTime.of(2026, 7, 20, 10, 30),
                UUID.randomUUID(),
                "Ahmat",
                UUID.randomUUID(),
                "Moussa",
                null,
                "+23566123456",
                List.of(),
                xaf("50000"),
                xaf("50000"),
                xaf("0"),
                true,
                List.of()));

        // Une créance soldée reste consultable : c'est la preuve du règlement.
        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").value(true))
                .andExpect(jsonPath("$.amountDue.amount").value("0"));
    }

    @Test
    void should_return_404_for_a_cash_sale_which_is_not_a_debt() throws Exception {
        UUID saleId = UUID.randomUUID();

        given(getCreditSaleDetailUseCase.detailOf(SaleId.of(saleId)))
                .willThrow(new SaleNotFoundException(SaleId.of(saleId)));

        mockMvc.perform(get("/api/v1/debts/{saleId}", saleId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SALE_NOT_FOUND"));
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
