package com.aliCheikh.stock.application.dto.dashboard;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Dashboard read model that does not expose domain entities. */
public record DashboardSummary(
        LocalDateTime generatedAt,
        Periods periods,
        Attention attention,
        StockSummary stock,
        SalesSummary sales,
        DebtSummary debts,
        List<RecentActivity> recentActivity
) {
    public DashboardSummary {
        Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        Objects.requireNonNull(periods, "periods cannot be null");
        Objects.requireNonNull(attention, "attention cannot be null");
        Objects.requireNonNull(stock, "stock cannot be null");
        Objects.requireNonNull(sales, "sales cannot be null");
        Objects.requireNonNull(debts, "debts cannot be null");
        recentActivity = List.copyOf(Objects.requireNonNull(
                recentActivity, "recentActivity cannot be null"));
    }

    public record Periods(
            LocalDateTime todayStart,
            LocalDateTime tomorrowStart,
            LocalDateTime last7DaysStart,
            LocalDateTime previous7DaysStart
    ) {
    }

    public record Attention(long outOfStockCount,
                            long belowThresholdCount,
                            long overdueCustomerCount) {
    }

    public record StockSummary(long outOfStockCount,
                               long belowThresholdCount,
                               List<StockAlert> alerts) {
        public StockSummary {
            alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts cannot be null"));
        }
    }

    public record StockAlert(UUID productId,
                             String reference,
                             String name,
                             long availableQuantity,
                             int threshold,
                             long shortage,
                             StockAlertStatus status) {
        public StockAlert {
            Objects.requireNonNull(productId, "productId cannot be null");
            Objects.requireNonNull(reference, "reference cannot be null");
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(status, "status cannot be null");
        }
    }

    public enum StockAlertStatus {
        OUT_OF_STOCK,
        BELOW_THRESHOLD
    }

    public record SalesSummary(SalesPeriod today,
                               SalesPeriod last7Days,
                               BigDecimal revenueChangePercent,
                               TopProduct bestSeller,
                               List<TopProduct> topProducts) {
        public SalesSummary {
            Objects.requireNonNull(today, "today cannot be null");
            Objects.requireNonNull(last7Days, "last7Days cannot be null");
            topProducts = List.copyOf(Objects.requireNonNull(
                    topProducts, "topProducts cannot be null"));
        }
    }

    public record SalesPeriod(long saleCount,
                              long itemsSold,
                              Money revenue,
                              Money averageBasket) {
        public SalesPeriod {
            Objects.requireNonNull(revenue, "revenue cannot be null");
            Objects.requireNonNull(averageBasket, "averageBasket cannot be null");
        }
    }

    public record TopProduct(UUID productId,
                             String reference,
                             String name,
                             long quantitySold,
                             Money revenue) {
        public TopProduct {
            Objects.requireNonNull(productId, "productId cannot be null");
            Objects.requireNonNull(reference, "reference cannot be null");
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(revenue, "revenue cannot be null");
        }
    }

    public record DebtSummary(long openDebtCount,
                              long openCustomerCount,
                              long overdueCustomerCount,
                              Money totalOutstanding,
                              List<DebtAging> aging,
                              List<CustomerDebtAlert> customersToContact) {
        public DebtSummary {
            Objects.requireNonNull(totalOutstanding, "totalOutstanding cannot be null");
            aging = List.copyOf(Objects.requireNonNull(aging, "aging cannot be null"));
            customersToContact = List.copyOf(Objects.requireNonNull(
                    customersToContact, "customersToContact cannot be null"));
        }
    }

    public record DebtAging(DebtAgeBand band, long debtCount, Money amountDue) {
        public DebtAging {
            Objects.requireNonNull(band, "band cannot be null");
            Objects.requireNonNull(amountDue, "amountDue cannot be null");
        }
    }

    public enum DebtAgeBand {
        DAYS_0_7,
        DAYS_8_14,
        DAYS_15_30,
        OVER_30_DAYS
    }

    public record CustomerDebtAlert(UUID customerId,
                                    String givenName,
                                    String fatherName,
                                    String phoneNumber,
                                    long openDebtCount,
                                    Money totalDue,
                                    UUID oldestSaleId,
                                    LocalDateTime oldestSaleAt,
                                    long daysOutstanding) {
        public CustomerDebtAlert {
            Objects.requireNonNull(customerId, "customerId cannot be null");
            Objects.requireNonNull(givenName, "givenName cannot be null");
            Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
            Objects.requireNonNull(totalDue, "totalDue cannot be null");
            Objects.requireNonNull(oldestSaleId, "oldestSaleId cannot be null");
            Objects.requireNonNull(oldestSaleAt, "oldestSaleAt cannot be null");
        }
    }

    public record RecentActivity(ActivityType type,
                                 UUID resourceId,
                                 LocalDateTime occurredAt,
                                 String actorName,
                                 long itemCount,
                                 long quantity,
                                 Money amount) {
        public RecentActivity {
            Objects.requireNonNull(type, "type cannot be null");
            Objects.requireNonNull(resourceId, "resourceId cannot be null");
            Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
            Objects.requireNonNull(actorName, "actorName cannot be null");
        }
    }

    public enum ActivityType {
        SALE,
        STOCK_RECEIPT,
        STOCK_TRANSFER,
        DEBT_PAYMENT
    }
}
