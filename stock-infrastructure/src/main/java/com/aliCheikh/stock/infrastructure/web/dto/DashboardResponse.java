package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Bounded dashboard REST contract independent of domain objects. */
public record DashboardResponse(
        LocalDateTime generatedAt,
        PeriodsResponse periods,
        AttentionResponse attention,
        StockSummaryResponse stock,
        SalesSummaryResponse sales,
        DebtSummaryResponse debts,
        List<RecentActivityResponse> recentActivity
) {
    public record PeriodsResponse(
            LocalDateTime todayStart,
            LocalDateTime tomorrowStart,
            LocalDateTime last7DaysStart,
            LocalDateTime previous7DaysStart
    ) {
    }

    public record AttentionResponse(
            long outOfStockCount,
            long belowThresholdCount,
            long overdueCustomerCount
    ) {
    }

    public record StockSummaryResponse(
            long outOfStockCount,
            long belowThresholdCount,
            List<StockAlertResponse> alerts
    ) {
    }

    public record StockAlertResponse(
            UUID productId,
            String reference,
            String name,
            long availableQuantity,
            int threshold,
            long shortage,
            DashboardSummary.StockAlertStatus status
    ) {
    }

    public record SalesSummaryResponse(
            SalesPeriodResponse today,
            SalesPeriodResponse last7Days,
            BigDecimal revenueChangePercent,
            TopProductResponse bestSeller,
            List<TopProductResponse> topProducts
    ) {
    }

    public record SalesPeriodResponse(
            long saleCount,
            long itemsSold,
            MoneyResponse revenue,
            MoneyResponse averageBasket
    ) {
    }

    public record TopProductResponse(
            UUID productId,
            String reference,
            String name,
            long quantitySold,
            MoneyResponse revenue
    ) {
    }

    public record DebtSummaryResponse(
            long openDebtCount,
            long openCustomerCount,
            long overdueCustomerCount,
            MoneyResponse totalOutstanding,
            List<DebtAgingResponse> aging,
            List<CustomerDebtAlertResponse> customersToContact
    ) {
    }

    public record DebtAgingResponse(
            DashboardSummary.DebtAgeBand band,
            long debtCount,
            MoneyResponse amountDue
    ) {
    }

    public record CustomerDebtAlertResponse(
            UUID customerId,
            String givenName,
            String fatherName,
            String phoneNumber,
            long openDebtCount,
            MoneyResponse totalDue,
            UUID oldestSaleId,
            LocalDateTime oldestSaleAt,
            long daysOutstanding
    ) {
    }

    public record RecentActivityResponse(
            DashboardSummary.ActivityType type,
            UUID resourceId,
            LocalDateTime occurredAt,
            String actorName,
            long itemCount,
            long quantity,
            MoneyResponse amount
    ) {
    }
}
