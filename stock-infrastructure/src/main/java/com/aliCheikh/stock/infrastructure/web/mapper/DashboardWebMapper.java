package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;
import com.aliCheikh.stock.infrastructure.web.dto.DashboardResponse;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;

import java.util.Objects;

public final class DashboardWebMapper {

    private DashboardWebMapper() {
    }

    public static DashboardResponse toResponse(DashboardSummary summary) {
        Objects.requireNonNull(summary, "summary cannot be null");

        return new DashboardResponse(
                summary.generatedAt(),
                new DashboardResponse.PeriodsResponse(
                        summary.periods().todayStart(),
                        summary.periods().tomorrowStart(),
                        summary.periods().last7DaysStart(),
                        summary.periods().previous7DaysStart()),
                new DashboardResponse.AttentionResponse(
                        summary.attention().outOfStockCount(),
                        summary.attention().belowThresholdCount(),
                        summary.attention().overdueCustomerCount()),
                new DashboardResponse.StockSummaryResponse(
                        summary.stock().outOfStockCount(),
                        summary.stock().belowThresholdCount(),
                        summary.stock().alerts().stream()
                                .map(DashboardWebMapper::stockAlert)
                                .toList()),
                new DashboardResponse.SalesSummaryResponse(
                        salesPeriod(summary.sales().today()),
                        salesPeriod(summary.sales().last7Days()),
                        summary.sales().revenueChangePercent(),
                        topProduct(summary.sales().bestSeller()),
                        summary.sales().topProducts().stream()
                                .map(DashboardWebMapper::topProduct)
                                .toList()),
                new DashboardResponse.DebtSummaryResponse(
                        summary.debts().openDebtCount(),
                        summary.debts().openCustomerCount(),
                        summary.debts().overdueCustomerCount(),
                        MoneyResponse.from(summary.debts().totalOutstanding()),
                        summary.debts().aging().stream()
                                .map(DashboardWebMapper::debtAging)
                                .toList(),
                        summary.debts().customersToContact().stream()
                                .map(DashboardWebMapper::customerDebtAlert)
                                .toList()),
                summary.recentActivity().stream()
                        .map(DashboardWebMapper::recentActivity)
                        .toList());
    }

    private static DashboardResponse.StockAlertResponse stockAlert(
            DashboardSummary.StockAlert alert) {
        return new DashboardResponse.StockAlertResponse(
                alert.productId(),
                alert.reference(),
                alert.name(),
                alert.availableQuantity(),
                alert.threshold(),
                alert.shortage(),
                alert.status());
    }

    private static DashboardResponse.SalesPeriodResponse salesPeriod(
            DashboardSummary.SalesPeriod period) {
        return new DashboardResponse.SalesPeriodResponse(
                period.saleCount(),
                period.itemsSold(),
                MoneyResponse.from(period.revenue()),
                MoneyResponse.from(period.averageBasket()));
    }

    private static DashboardResponse.TopProductResponse topProduct(
            DashboardSummary.TopProduct product) {
        if (product == null) {
            return null;
        }
        return new DashboardResponse.TopProductResponse(
                product.productId(),
                product.reference(),
                product.name(),
                product.quantitySold(),
                MoneyResponse.from(product.revenue()));
    }

    private static DashboardResponse.DebtAgingResponse debtAging(
            DashboardSummary.DebtAging aging) {
        return new DashboardResponse.DebtAgingResponse(
                aging.band(),
                aging.debtCount(),
                MoneyResponse.from(aging.amountDue()));
    }

    private static DashboardResponse.CustomerDebtAlertResponse customerDebtAlert(
            DashboardSummary.CustomerDebtAlert alert) {
        return new DashboardResponse.CustomerDebtAlertResponse(
                alert.customerId(),
                alert.givenName(),
                alert.fatherName(),
                alert.phoneNumber(),
                alert.openDebtCount(),
                MoneyResponse.from(alert.totalDue()),
                alert.oldestSaleId(),
                alert.oldestSaleAt(),
                alert.daysOutstanding());
    }

    private static DashboardResponse.RecentActivityResponse recentActivity(
            DashboardSummary.RecentActivity activity) {
        return new DashboardResponse.RecentActivityResponse(
                activity.type(),
                activity.resourceId(),
                activity.occurredAt(),
                activity.actorName(),
                activity.itemCount(),
                activity.quantity(),
                MoneyResponse.from(activity.amount()));
    }
}
