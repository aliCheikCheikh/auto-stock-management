package com.aliCheikh.stock.application.dto.dashboard;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Objects;

/** Query boundaries computed once using the business clock. */
public record DashboardQueryCriteria(
        LocalDateTime now,
        LocalDateTime todayStart,
        LocalDateTime tomorrowStart,
        LocalDateTime last7DaysStart,
        LocalDateTime previous7DaysStart,
        LocalDateTime overdueAtOrBefore,
        Currency currency,
        int alertLimit,
        int activityLimit
) {
    public DashboardQueryCriteria {
        Objects.requireNonNull(now, "now cannot be null");
        Objects.requireNonNull(todayStart, "todayStart cannot be null");
        Objects.requireNonNull(tomorrowStart, "tomorrowStart cannot be null");
        Objects.requireNonNull(last7DaysStart, "last7DaysStart cannot be null");
        Objects.requireNonNull(previous7DaysStart, "previous7DaysStart cannot be null");
        Objects.requireNonNull(overdueAtOrBefore, "overdueAtOrBefore cannot be null");
        Objects.requireNonNull(currency, "currency cannot be null");

        if (!previous7DaysStart.isBefore(last7DaysStart)
                || !last7DaysStart.isBefore(todayStart)
                || !todayStart.isBefore(tomorrowStart)) {
            throw new IllegalArgumentException("dashboard periods must be strictly ordered");
        }
        if (alertLimit < 1 || activityLimit < 1) {
            throw new IllegalArgumentException("dashboard limits must be greater than zero");
        }
    }
}
