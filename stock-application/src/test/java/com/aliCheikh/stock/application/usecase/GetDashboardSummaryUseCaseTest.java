package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.dashboard.DashboardQueryCriteria;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSnapshot;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;
import com.aliCheikh.stock.application.port.DashboardQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class GetDashboardSummaryUseCaseTest {

    private static final ZoneId NDJAMENA = ZoneId.of("Africa/Ndjamena");
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Instant NOW = Instant.parse("2026-08-05T23:30:00Z");

    private DashboardQueryPort dashboardQueryPort;
    private GetDashboardSummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        dashboardQueryPort = Mockito.mock(DashboardQueryPort.class);
        useCase = new GetDashboardSummaryUseCase(
                dashboardQueryPort,
                Clock.fixed(NOW, NDJAMENA),
                XAF);
    }

    @Test
    void should_build_calendar_periods_in_the_business_timezone() {
        given(dashboardQueryPort.load(Mockito.any())).willReturn(emptySnapshot());

        DashboardSummary result = useCase.execute();

        ArgumentCaptor<DashboardQueryCriteria> captor =
                ArgumentCaptor.forClass(DashboardQueryCriteria.class);
        verify(dashboardQueryPort).load(captor.capture());
        DashboardQueryCriteria criteria = captor.getValue();

        // It is still August 5 in UTC but already August 6 in N'Djamena.
        assertThat(result.generatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 6, 0, 30));
        assertThat(criteria.todayStart()).isEqualTo(LocalDateTime.of(2026, 8, 6, 0, 0));
        assertThat(criteria.tomorrowStart()).isEqualTo(LocalDateTime.of(2026, 8, 7, 0, 0));
        assertThat(criteria.last7DaysStart()).isEqualTo(LocalDateTime.of(2026, 7, 31, 0, 0));
        assertThat(criteria.previous7DaysStart()).isEqualTo(LocalDateTime.of(2026, 7, 24, 0, 0));
        assertThat(criteria.overdueAtOrBefore())
                .isEqualTo(LocalDateTime.of(2026, 7, 6, 0, 30));
        assertThat(criteria.currency()).isEqualTo(XAF);
        assertThat(criteria.alertLimit()).isEqualTo(5);
        assertThat(criteria.activityLimit()).isEqualTo(8);
    }

    @Test
    void should_derive_the_attention_strip_from_authoritative_backend_totals() {
        DashboardSnapshot snapshot = new DashboardSnapshot(
                new DashboardSummary.StockSummary(3, 7, List.of()),
                emptySales(),
                new DashboardSummary.DebtSummary(
                        12, 6, 4, xaf("350000"), List.of(), List.of()),
                List.of());
        given(dashboardQueryPort.load(Mockito.any())).willReturn(snapshot);

        DashboardSummary result = useCase.execute();

        assertThat(result.attention()).isEqualTo(new DashboardSummary.Attention(3, 7, 4));
        assertThat(result.stock()).isSameAs(snapshot.stock());
        assertThat(result.sales()).isSameAs(snapshot.sales());
        assertThat(result.debts()).isSameAs(snapshot.debts());
    }

    private static DashboardSnapshot emptySnapshot() {
        return new DashboardSnapshot(
                new DashboardSummary.StockSummary(0, 0, List.of()),
                emptySales(),
                new DashboardSummary.DebtSummary(0, 0, 0, xaf("0"), List.of(), List.of()),
                List.of());
    }

    private static DashboardSummary.SalesSummary emptySales() {
        DashboardSummary.SalesPeriod emptyPeriod =
                new DashboardSummary.SalesPeriod(0, 0, xaf("0"), xaf("0"));
        return new DashboardSummary.SalesSummary(
                emptyPeriod, emptyPeriod, null, null, List.of());
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
