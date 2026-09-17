package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.RecordPaymentCommand;
import com.aliCheikh.stock.application.dto.RecordPaymentResult;
import com.aliCheikh.stock.application.usecase.ListSalesUseCase;
import com.aliCheikh.stock.application.usecase.RecordPaymentUseCase;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.domain.exception.sale.PaymentExceedsAmountDueException;
import com.aliCheikh.stock.domain.exception.sale.SaleAlreadySettledException;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Payment web slice. Set the request principal directly because security filters are disabled and
 * cannot populate Authentication.
 */
@WebMvcTest(SaleController.class)
@AutoConfigureMockMvc(addFilters = false)
class SalePaymentControllerTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordPaymentUseCase recordPaymentUseCase;

    @MockitoBean
    private SellProductUseCase sellProductUseCase;

    @MockitoBean
    private SaleRepository saleRepository;

    @MockitoBean
    private ListSalesUseCase listSalesUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    private static Money xaf(long amount) {
        return Money.create(BigDecimal.valueOf(amount), XAF);
    }

    private static UsernamePasswordAuthenticationToken cashier(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    void should_record_a_payment_and_return_the_remaining_balance() throws Exception {
        UUID saleId = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();

        given(recordPaymentUseCase.record(any())).willReturn(new RecordPaymentResult(
                SaleId.of(saleId),
                xaf(12_000),
                LocalDateTime.of(2026, 8, 15, 10, 0),
                xaf(50_000),
                xaf(32_000),
                xaf(18_000),
                false));

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .principal(cashier(cashierId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 12000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.amountPaid.amount").value("12000"))
                .andExpect(jsonPath("$.amountDue.amount").value("18000"))
                .andExpect(jsonPath("$.settled").value(false));
    }

    @Test
    void should_take_the_cashier_from_the_token_and_never_from_the_body() throws Exception {
        UUID saleId = UUID.randomUUID();
        UUID cashierId = UUID.randomUUID();

        given(recordPaymentUseCase.record(any())).willReturn(new RecordPaymentResult(
                SaleId.of(saleId), xaf(1_000), LocalDateTime.now(),
                xaf(50_000), xaf(1_000), xaf(49_000), false));

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .principal(cashier(cashierId))
                        .contentType(MediaType.APPLICATION_JSON)
                        // A receiver ID supplied in the body must not override the authenticated
                        // user.
                        .content("{\"amount\": 1000, \"receivedBy\": \"11111111-1111-1111-1111-111111111111\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<RecordPaymentCommand> captor = ArgumentCaptor.forClass(RecordPaymentCommand.class);
        verify(recordPaymentUseCase).record(captor.capture());
        assertThat(captor.getValue().receivedBy().getValue()).isEqualTo(cashierId);
    }

    @Test
    void should_return_422_when_the_payment_exceeds_the_balance() throws Exception {
        UUID saleId = UUID.randomUUID();

        given(recordPaymentUseCase.record(any()))
                .willThrow(new PaymentExceedsAmountDueException(xaf(99_000), xaf(18_000)));

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .principal(cashier(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 99000}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_EXCEEDS_AMOUNT_DUE"));
    }

    @Test
    void should_return_409_when_the_sale_is_already_settled() throws Exception {
        UUID saleId = UUID.randomUUID();

        given(recordPaymentUseCase.record(any()))
                .willThrow(new SaleAlreadySettledException(SaleId.of(saleId)));

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .principal(cashier(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SALE_ALREADY_SETTLED"));
    }

    @Test
    void should_reject_a_zero_or_negative_amount_before_reaching_the_domain() throws Exception {
        UUID saleId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/sales/{saleId}/payments", saleId)
                        .principal(cashier(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest());
    }
}
