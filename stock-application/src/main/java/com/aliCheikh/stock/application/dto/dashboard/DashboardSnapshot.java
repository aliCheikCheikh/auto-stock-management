package com.aliCheikh.stock.application.dto.dashboard;

import java.util.List;
import java.util.Objects;

/** Données autoritaires lues en base, avant ajout du contexte temporel par le use case. */
public record DashboardSnapshot(
        DashboardSummary.StockSummary stock,
        DashboardSummary.SalesSummary sales,
        DashboardSummary.DebtSummary debts,
        List<DashboardSummary.RecentActivity> recentActivity
) {
    public DashboardSnapshot {
        Objects.requireNonNull(stock, "stock cannot be null");
        Objects.requireNonNull(sales, "sales cannot be null");
        Objects.requireNonNull(debts, "debts cannot be null");
        recentActivity = List.copyOf(Objects.requireNonNull(
                recentActivity, "recentActivity cannot be null"));
    }
}
