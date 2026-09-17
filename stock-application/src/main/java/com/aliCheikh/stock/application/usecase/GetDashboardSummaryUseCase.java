package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.dashboard.DashboardQueryCriteria;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSnapshot;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;
import com.aliCheikh.stock.application.port.DashboardQueryPort;
import com.aliCheikh.stock.domain.service.CreditPolicy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Objects;

/** Builds a bounded dashboard snapshot using the shop's business clock. */
public class GetDashboardSummaryUseCase {

    static final int ALERT_LIMIT = 5;
    static final int ACTIVITY_LIMIT = 8;

    private final DashboardQueryPort dashboardQueryPort;
    private final Clock clock;
    private final Currency businessCurrency;

    public GetDashboardSummaryUseCase(DashboardQueryPort dashboardQueryPort,
                                      Clock clock,
                                      Currency businessCurrency) {
        this.dashboardQueryPort = Objects.requireNonNull(
                dashboardQueryPort, "dashboardQueryPort cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
        this.businessCurrency = Objects.requireNonNull(
                businessCurrency, "businessCurrency cannot be null");
    }

    public DashboardSummary execute() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        DashboardQueryCriteria criteria = new DashboardQueryCriteria(
                now,
                todayStart,
                tomorrowStart,
                todayStart.minusDays(6),
                todayStart.minusDays(13),
                now.minusDays(CreditPolicy.OVERDUE_AFTER_DAYS + 1L),
                businessCurrency,
                ALERT_LIMIT,
                ACTIVITY_LIMIT);

        DashboardSnapshot snapshot = dashboardQueryPort.load(criteria);

        return new DashboardSummary(
                now,
                new DashboardSummary.Periods(
                        criteria.todayStart(),
                        criteria.tomorrowStart(),
                        criteria.last7DaysStart(),
                        criteria.previous7DaysStart()),
                new DashboardSummary.Attention(
                        snapshot.stock().outOfStockCount(),
                        snapshot.stock().belowThresholdCount(),
                        snapshot.debts().overdueCustomerCount()),
                snapshot.stock(),
                snapshot.sales(),
                snapshot.debts(),
                snapshot.recentActivity());
    }
}
