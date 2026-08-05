package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;
import com.aliCheikh.stock.application.usecase.GetDashboardSummaryUseCase;
import com.aliCheikh.stock.application.usecase.GetUserUseCase;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import com.aliCheikh.stock.infrastructure.security.JwtService;
import com.aliCheikh.stock.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDashboardSummaryUseCase getDashboardSummaryUseCase;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 10, 15);
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        Money zero = Money.zero(Currency.getInstance("XAF"));
        given(getDashboardSummaryUseCase.execute()).willReturn(new DashboardSummary(
                now,
                new DashboardSummary.Periods(
                        today, today.plusDays(1), today.minusDays(6), today.minusDays(13)),
                new DashboardSummary.Attention(0, 0, 0),
                new DashboardSummary.StockSummary(0, 0, List.of()),
                new DashboardSummary.SalesSummary(
                        new DashboardSummary.SalesPeriod(0, 0, zero, zero),
                        new DashboardSummary.SalesPeriod(0, 0, zero, zero),
                        null,
                        null,
                        List.of()),
                new DashboardSummary.DebtSummary(0, 0, 0, zero, List.of(), List.of()),
                List.of()));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void should_allow_the_owner_to_read_the_dashboard() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void should_forbid_a_seller_from_reading_management_indicators() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_require_authentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }
}
