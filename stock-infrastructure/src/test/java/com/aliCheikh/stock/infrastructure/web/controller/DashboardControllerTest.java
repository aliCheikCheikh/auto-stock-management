package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;
import com.aliCheikh.stock.application.usecase.GetDashboardSummaryUseCase;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDashboardSummaryUseCase getDashboardSummaryUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Test
    void should_expose_a_bounded_action_oriented_summary() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID saleId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 10, 15);

        given(getDashboardSummaryUseCase.execute()).willReturn(summary(
                now, productId, customerId, saleId));

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").value("2026-08-06T10:15:00"))
                .andExpect(jsonPath("$.attention.outOfStockCount").value(2))
                .andExpect(jsonPath("$.attention.belowThresholdCount").value(3))
                .andExpect(jsonPath("$.attention.overdueCustomerCount").value(1))
                .andExpect(jsonPath("$.stock.alerts[0].productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.stock.alerts[0].status").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.sales.today.revenue.amount").value("125000"))
                .andExpect(jsonPath("$.sales.today.revenue.currency").value("XAF"))
                .andExpect(jsonPath("$.sales.bestSeller.productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.debts.totalOutstanding.amount").value("80000"))
                .andExpect(jsonPath("$.debts.customersToContact[0].customerId")
                        .value(customerId.toString()))
                .andExpect(jsonPath("$.recentActivity[0].resourceId")
                        .value(saleId.toString()))
                .andExpect(jsonPath("$.recentActivity[0].amount.amount").value("50000"));
    }

    @Test
    void should_expose_empty_lists_and_nullable_comparisons_without_fake_values() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 6, 10, 15);
        DashboardSummary empty = new DashboardSummary(
                now,
                periods(now),
                new DashboardSummary.Attention(0, 0, 0),
                new DashboardSummary.StockSummary(0, 0, List.of()),
                new DashboardSummary.SalesSummary(
                        salesPeriod("0", 0, 0),
                        salesPeriod("0", 0, 0),
                        null,
                        null,
                        List.of()),
                new DashboardSummary.DebtSummary(
                        0, 0, 0, xaf("0"), List.of(), List.of()),
                List.of());
        given(getDashboardSummaryUseCase.execute()).willReturn(empty);

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock.alerts").isEmpty())
                .andExpect(jsonPath("$.sales.revenueChangePercent").doesNotExist())
                .andExpect(jsonPath("$.sales.bestSeller").doesNotExist())
                .andExpect(jsonPath("$.debts.customersToContact").isEmpty())
                .andExpect(jsonPath("$.recentActivity").isEmpty());
    }

    private static DashboardSummary summary(LocalDateTime now,
                                              UUID productId,
                                              UUID customerId,
                                              UUID saleId) {
        DashboardSummary.TopProduct topProduct = new DashboardSummary.TopProduct(
                productId, "ALT-001", "Alternateur", 7, xaf("350000"));

        return new DashboardSummary(
                now,
                periods(now),
                new DashboardSummary.Attention(2, 3, 1),
                new DashboardSummary.StockSummary(2, 3, List.of(
                        new DashboardSummary.StockAlert(
                                productId,
                                "ALT-001",
                                "Alternateur",
                                0,
                                4,
                                4,
                                DashboardSummary.StockAlertStatus.OUT_OF_STOCK))),
                new DashboardSummary.SalesSummary(
                        salesPeriod("125000", 3, 5),
                        salesPeriod("600000", 12, 18),
                        new BigDecimal("12.50"),
                        topProduct,
                        List.of(topProduct)),
                new DashboardSummary.DebtSummary(
                        2,
                        1,
                        1,
                        xaf("80000"),
                        List.of(new DashboardSummary.DebtAging(
                                DashboardSummary.DebtAgeBand.OVER_30_DAYS,
                                1,
                                xaf("80000"))),
                        List.of(new DashboardSummary.CustomerDebtAlert(
                                customerId,
                                "Moussa",
                                "Mahamat",
                                "+23566000001",
                                2,
                                xaf("80000"),
                                saleId,
                                now.minusDays(40),
                                40))),
                List.of(new DashboardSummary.RecentActivity(
                        DashboardSummary.ActivityType.SALE,
                        saleId,
                        now.minusMinutes(5),
                        "Amina",
                        2,
                        3,
                        xaf("50000"))));
    }

    private static DashboardSummary.Periods periods(LocalDateTime now) {
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        return new DashboardSummary.Periods(
                today,
                today.plusDays(1),
                today.minusDays(6),
                today.minusDays(13));
    }

    private static DashboardSummary.SalesPeriod salesPeriod(String revenue,
                                                             long sales,
                                                             long items) {
        Money amount = xaf(revenue);
        Money average = sales == 0
                ? xaf("0")
                : xaf(new BigDecimal(revenue)
                        .divide(BigDecimal.valueOf(sales), 2, RoundingMode.HALF_UP)
                        .toPlainString());
        return new DashboardSummary.SalesPeriod(sales, items, amount, average);
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
