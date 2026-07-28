package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.OutstandingDebtView;
import com.aliCheikh.stock.application.usecase.ListOutstandingDebtsUseCase;
import com.aliCheikh.stock.domain.model.shared.Money;
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

    @Test
    void should_expose_outstanding_debts_with_their_balance() throws Exception {
        UUID saleId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        given(listOutstandingDebtsUseCase.listAll()).willReturn(List.of(new OutstandingDebtView(
                saleId,
                LocalDateTime.of(2026, 7, 20, 10, 30),
                customerId,
                "Ahmat",
                "Youssouf",
                "+23566123456",
                xaf("50000"),
                xaf("20000"),
                xaf("30000"))));

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

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
