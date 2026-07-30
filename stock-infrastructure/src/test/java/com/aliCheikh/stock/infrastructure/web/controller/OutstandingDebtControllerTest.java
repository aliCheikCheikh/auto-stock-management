package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.application.dto.CreditSaleLineView;
import com.aliCheikh.stock.application.dto.CreditSalePaymentView;
import com.aliCheikh.stock.application.dto.OutstandingDebtSummary;
import com.aliCheikh.stock.application.usecase.GetCreditSaleDetailUseCase;
import com.aliCheikh.stock.application.usecase.ListOutstandingDebtsUseCase;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutstandingDebtController.class)
@AutoConfigureMockMvc(addFilters = false)
class OutstandingDebtControllerTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListOutstandingDebtsUseCase listOutstandingDebtsUseCase;

    @MockitoBean
    private GetCreditSaleDetailUseCase getCreditSaleDetailUseCase;

    /** Requis par IdempotencyFilter, chargé par la tranche web mais dépendant de la persistance. */
    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Test
    void should_expose_outstanding_debts_with_their_balance() throws Exception {
        UUID saleId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        given(listOutstandingDebtsUseCase.listAll()).willReturn(List.of(new OutstandingDebtSummary(
                saleId,
                LocalDateTime.of(2026, 7, 20, 10, 30),
                customerId,
                "Ahmat",
                "Youssouf",
                "+23566123456",
                xaf("50000"),
                xaf("20000"),
                xaf("30000"),
                45,
                true)));

        mockMvc.perform(get("/api/v1/debts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$[0].customerGivenName").value("Ahmat"))
                .andExpect(jsonPath("$[0].customerPhoneNumber").value("+23566123456"))
                .andExpect(jsonPath("$[0].amountDue.amount").value("30000"))
                .andExpect(jsonPath("$[0].amountDue.currency").value("XAF"));
    }

    @Test
    void should_return_an_empty_list_when_nobody_owes_anything() throws Exception {
        given(listOutstandingDebtsUseCase.listAll()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/debts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
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
