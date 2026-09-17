package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.dashboard.DashboardQueryCriteria;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSnapshot;
import com.aliCheikh.stock.application.dto.dashboard.DashboardSummary;
import com.aliCheikh.stock.application.port.DashboardQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.service.CreditPolicy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Dashboard SQL read model. Aggregation runs in PostgreSQL rather than summing incomplete pages in
 * Java.
 */
@Repository
public class DashboardQuerySqlAdapter implements DashboardQueryPort {

    private static final String STOCK_QUANTITIES = """
            WITH stock_quantities AS (
                SELECT p.id,
                       p.reference,
                       p.name,
                       p.minimum_global_threshold AS threshold,
                       COALESCE(SUM(sl.quantity), 0) AS available_quantity
                FROM product p
                LEFT JOIN stock_level sl ON sl.product_id = p.id
                WHERE p.active = TRUE
                GROUP BY p.id, p.reference, p.name, p.minimum_global_threshold
            )
            """;

    private static final String OPEN_DEBTS = """
            WITH open_debts AS (
                SELECT s.id AS sale_id,
                       s.occurred_at,
                       c.id AS customer_id,
                       c.given_name,
                       c.father_name,
                       c.phone_number,
                       s.total_amount - COALESCE(SUM(p.amount), 0) AS amount_due
                FROM sale s
                JOIN customer c ON c.id = s.customer_id
                LEFT JOIN payment p ON p.sale_id = s.id
                WHERE s.total_currency = :currency
                GROUP BY s.id,
                         s.occurred_at,
                         c.id,
                         c.given_name,
                         c.father_name,
                         c.phone_number,
                         s.total_amount
                HAVING s.total_amount - COALESCE(SUM(p.amount), 0) > 0
            )
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardQuerySqlAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSnapshot load(DashboardQueryCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria cannot be null");

        return new DashboardSnapshot(
                loadStock(criteria),
                loadSales(criteria),
                loadDebts(criteria),
                loadRecentActivity(criteria));
    }

    private DashboardSummary.StockSummary loadStock(DashboardQueryCriteria criteria) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("limit", criteria.alertLimit());

        StockCounts counts = jdbc.queryForObject(
                STOCK_QUANTITIES + """
                        SELECT COUNT(*) FILTER (WHERE available_quantity = 0) AS out_of_stock,
                               COUNT(*) FILTER (
                                   WHERE available_quantity > 0
                                     AND available_quantity < threshold
                               ) AS below_threshold
                        FROM stock_quantities
                        """,
                parameters,
                (resultSet, rowNumber) -> new StockCounts(
                        resultSet.getLong("out_of_stock"),
                        resultSet.getLong("below_threshold")));

        List<DashboardSummary.StockAlert> alerts = jdbc.query(
                STOCK_QUANTITIES + """
                        SELECT id,
                               reference,
                               name,
                               available_quantity,
                               threshold,
                               GREATEST(threshold - available_quantity, 0) AS shortage,
                               CASE WHEN available_quantity = 0
                                    THEN 'OUT_OF_STOCK'
                                    ELSE 'BELOW_THRESHOLD'
                               END AS status
                        FROM stock_quantities
                        WHERE available_quantity = 0
                           OR available_quantity < threshold
                        ORDER BY CASE WHEN available_quantity = 0 THEN 0 ELSE 1 END,
                                 GREATEST(threshold - available_quantity, 0) DESC,
                                 name ASC,
                                 id ASC
                        LIMIT :limit
                        """,
                parameters,
                (resultSet, rowNumber) -> new DashboardSummary.StockAlert(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("reference"),
                        resultSet.getString("name"),
                        resultSet.getLong("available_quantity"),
                        resultSet.getInt("threshold"),
                        resultSet.getLong("shortage"),
                        DashboardSummary.StockAlertStatus.valueOf(resultSet.getString("status"))));

        return new DashboardSummary.StockSummary(
                counts.outOfStock(),
                counts.belowThreshold(),
                alerts);
    }

    private DashboardSummary.SalesSummary loadSales(DashboardQueryCriteria criteria) {
        MapSqlParameterSource parameters = commonParameters(criteria)
                .addValue("limit", criteria.alertLimit());

        SalesTotals totals = jdbc.queryForObject("""
                WITH sales_with_items AS (
                    SELECT s.id,
                           s.occurred_at,
                           s.total_amount,
                           COALESCE(SUM(sl.quantity), 0) AS items_sold
                    FROM sale s
                    LEFT JOIN sale_line sl ON sl.sale_id = s.id
                    WHERE s.total_currency = :currency
                      AND s.occurred_at >= :previous7DaysStart
                      AND s.occurred_at < :tomorrowStart
                    GROUP BY s.id, s.occurred_at, s.total_amount
                )
                SELECT COUNT(*) FILTER (
                           WHERE occurred_at >= :todayStart
                       ) AS today_sale_count,
                       COALESCE(SUM(items_sold) FILTER (
                           WHERE occurred_at >= :todayStart
                       ), 0) AS today_items_sold,
                       COALESCE(SUM(total_amount) FILTER (
                           WHERE occurred_at >= :todayStart
                       ), 0) AS today_revenue,
                       COUNT(*) FILTER (
                           WHERE occurred_at >= :last7DaysStart
                       ) AS last7_sale_count,
                       COALESCE(SUM(items_sold) FILTER (
                           WHERE occurred_at >= :last7DaysStart
                       ), 0) AS last7_items_sold,
                       COALESCE(SUM(total_amount) FILTER (
                           WHERE occurred_at >= :last7DaysStart
                       ), 0) AS last7_revenue,
                       COALESCE(SUM(total_amount) FILTER (
                           WHERE occurred_at < :last7DaysStart
                       ), 0) AS previous7_revenue
                FROM sales_with_items
                """, parameters, (resultSet, rowNumber) -> new SalesTotals(
                resultSet.getLong("today_sale_count"),
                resultSet.getLong("today_items_sold"),
                resultSet.getBigDecimal("today_revenue"),
                resultSet.getLong("last7_sale_count"),
                resultSet.getLong("last7_items_sold"),
                resultSet.getBigDecimal("last7_revenue"),
                resultSet.getBigDecimal("previous7_revenue")));

        List<DashboardSummary.TopProduct> topProducts = jdbc.query("""
                SELECT p.id,
                       p.reference,
                       p.name,
                       SUM(sl.quantity) AS quantity_sold,
                       SUM(sl.line_total_amount) AS revenue
                FROM sale s
                JOIN sale_line sl ON sl.sale_id = s.id
                JOIN product p ON p.id = sl.product_id
                WHERE s.total_currency = :currency
                  AND s.occurred_at >= :last7DaysStart
                  AND s.occurred_at < :tomorrowStart
                GROUP BY p.id, p.reference, p.name
                ORDER BY SUM(sl.quantity) DESC,
                         SUM(sl.line_total_amount) DESC,
                         p.name ASC,
                         p.id ASC
                LIMIT :limit
                """, parameters, (resultSet, rowNumber) -> new DashboardSummary.TopProduct(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("reference"),
                resultSet.getString("name"),
                resultSet.getLong("quantity_sold"),
                Money.create(resultSet.getBigDecimal("revenue"), criteria.currency())));

        DashboardSummary.SalesPeriod today = salesPeriod(
                totals.todaySaleCount(),
                totals.todayItemsSold(),
                totals.todayRevenue(),
                criteria);
        DashboardSummary.SalesPeriod last7Days = salesPeriod(
                totals.last7SaleCount(),
                totals.last7ItemsSold(),
                totals.last7Revenue(),
                criteria);

        return new DashboardSummary.SalesSummary(
                today,
                last7Days,
                percentageChange(totals.last7Revenue(), totals.previous7Revenue()),
                topProducts.isEmpty() ? null : topProducts.get(0),
                topProducts);
    }

    private static DashboardSummary.SalesPeriod salesPeriod(long saleCount,
                                                             long itemsSold,
                                                             BigDecimal revenue,
                                                             DashboardQueryCriteria criteria) {
        BigDecimal safeRevenue = revenue == null ? BigDecimal.ZERO : revenue;
        Money revenueMoney = Money.create(safeRevenue, criteria.currency());
        Money averageBasket = saleCount == 0
                ? Money.zero(criteria.currency())
                : Money.create(
                        safeRevenue.divide(BigDecimal.valueOf(saleCount), 2, RoundingMode.HALF_UP),
                        criteria.currency());
        return new DashboardSummary.SalesPeriod(
                saleCount, itemsSold, revenueMoney, averageBasket);
    }

    private static BigDecimal percentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private static MapSqlParameterSource commonParameters(DashboardQueryCriteria criteria) {
        return new MapSqlParameterSource()
                .addValue("currency", criteria.currency().getCurrencyCode())
                .addValue("todayStart", criteria.todayStart())
                .addValue("tomorrowStart", criteria.tomorrowStart())
                .addValue("last7DaysStart", criteria.last7DaysStart())
                .addValue("previous7DaysStart", criteria.previous7DaysStart())
                .addValue("now", criteria.now())
                .addValue("overdueAtOrBefore", criteria.overdueAtOrBefore());
    }

    private DashboardSummary.DebtSummary loadDebts(DashboardQueryCriteria criteria) {
        MapSqlParameterSource parameters = commonParameters(criteria)
                .addValue("limit", criteria.alertLimit());

        DebtTotals totals = jdbc.queryForObject(OPEN_DEBTS + """
                SELECT COUNT(*) AS open_debt_count,
                       COUNT(DISTINCT customer_id) AS open_customer_count,
                       COUNT(DISTINCT customer_id) FILTER (
                           WHERE occurred_at <= :overdueAtOrBefore
                       ) AS overdue_customer_count,
                       COALESCE(SUM(amount_due), 0) AS total_outstanding,
                       COUNT(*) FILTER (
                           WHERE occurred_at > :now - INTERVAL '8 days'
                       ) AS days_0_7_count,
                       COALESCE(SUM(amount_due) FILTER (
                           WHERE occurred_at > :now - INTERVAL '8 days'
                       ), 0) AS days_0_7_amount,
                       COUNT(*) FILTER (
                           WHERE occurred_at <= :now - INTERVAL '8 days'
                             AND occurred_at > :now - INTERVAL '15 days'
                       ) AS days_8_14_count,
                       COALESCE(SUM(amount_due) FILTER (
                           WHERE occurred_at <= :now - INTERVAL '8 days'
                             AND occurred_at > :now - INTERVAL '15 days'
                       ), 0) AS days_8_14_amount,
                       COUNT(*) FILTER (
                           WHERE occurred_at <= :now - INTERVAL '15 days'
                             AND occurred_at > :overdueAtOrBefore
                       ) AS days_15_30_count,
                       COALESCE(SUM(amount_due) FILTER (
                           WHERE occurred_at <= :now - INTERVAL '15 days'
                             AND occurred_at > :overdueAtOrBefore
                       ), 0) AS days_15_30_amount,
                       COUNT(*) FILTER (
                           WHERE occurred_at <= :overdueAtOrBefore
                       ) AS over_30_count,
                       COALESCE(SUM(amount_due) FILTER (
                           WHERE occurred_at <= :overdueAtOrBefore
                       ), 0) AS over_30_amount
                FROM open_debts
                """, parameters, (resultSet, rowNumber) -> new DebtTotals(
                resultSet.getLong("open_debt_count"),
                resultSet.getLong("open_customer_count"),
                resultSet.getLong("overdue_customer_count"),
                resultSet.getBigDecimal("total_outstanding"),
                resultSet.getLong("days_0_7_count"),
                resultSet.getBigDecimal("days_0_7_amount"),
                resultSet.getLong("days_8_14_count"),
                resultSet.getBigDecimal("days_8_14_amount"),
                resultSet.getLong("days_15_30_count"),
                resultSet.getBigDecimal("days_15_30_amount"),
                resultSet.getLong("over_30_count"),
                resultSet.getBigDecimal("over_30_amount")));

        List<DashboardSummary.CustomerDebtAlert> customersToContact = jdbc.query(
                OPEN_DEBTS + """
                        SELECT customer_id,
                               given_name,
                               father_name,
                               phone_number,
                               COUNT(*) AS open_debt_count,
                               SUM(amount_due) AS total_due,
                               (ARRAY_AGG(sale_id ORDER BY occurred_at ASC, sale_id ASC))[1]
                                   AS oldest_sale_id,
                               MIN(occurred_at) AS oldest_sale_at
                        FROM open_debts
                        GROUP BY customer_id, given_name, father_name, phone_number
                        HAVING MIN(occurred_at) <= :overdueAtOrBefore
                        ORDER BY SUM(amount_due) DESC,
                                 MIN(occurred_at) ASC,
                                 customer_id ASC
                        LIMIT :limit
                        """,
                parameters,
                (resultSet, rowNumber) -> {
                    var oldestSaleAt = resultSet.getTimestamp("oldest_sale_at").toLocalDateTime();
                    return new DashboardSummary.CustomerDebtAlert(
                            resultSet.getObject("customer_id", UUID.class),
                            resultSet.getString("given_name"),
                            resultSet.getString("father_name"),
                            resultSet.getString("phone_number"),
                            resultSet.getLong("open_debt_count"),
                            Money.create(resultSet.getBigDecimal("total_due"), criteria.currency()),
                            resultSet.getObject("oldest_sale_id", UUID.class),
                            oldestSaleAt,
                            CreditPolicy.daysOutstanding(oldestSaleAt, criteria.now()));
                });

        Money zero = Money.zero(criteria.currency());
        List<DashboardSummary.DebtAging> aging = List.of(
                new DashboardSummary.DebtAging(
                        DashboardSummary.DebtAgeBand.DAYS_0_7,
                        totals.days0To7Count(),
                        moneyOrZero(totals.days0To7Amount(), criteria, zero)),
                new DashboardSummary.DebtAging(
                        DashboardSummary.DebtAgeBand.DAYS_8_14,
                        totals.days8To14Count(),
                        moneyOrZero(totals.days8To14Amount(), criteria, zero)),
                new DashboardSummary.DebtAging(
                        DashboardSummary.DebtAgeBand.DAYS_15_30,
                        totals.days15To30Count(),
                        moneyOrZero(totals.days15To30Amount(), criteria, zero)),
                new DashboardSummary.DebtAging(
                        DashboardSummary.DebtAgeBand.OVER_30_DAYS,
                        totals.over30Count(),
                        moneyOrZero(totals.over30Amount(), criteria, zero)));

        return new DashboardSummary.DebtSummary(
                totals.openDebtCount(),
                totals.openCustomerCount(),
                totals.overdueCustomerCount(),
                moneyOrZero(totals.totalOutstanding(), criteria, zero),
                aging,
                customersToContact);
    }

    private static Money moneyOrZero(BigDecimal amount,
                                     DashboardQueryCriteria criteria,
                                     Money zero) {
        return amount == null ? zero : Money.create(amount, criteria.currency());
    }

    private List<DashboardSummary.RecentActivity> loadRecentActivity(
            DashboardQueryCriteria criteria) {
        MapSqlParameterSource parameters = commonParameters(criteria)
                .addValue("limit", criteria.activityLimit());

        return jdbc.query("""
                WITH sale_activities AS (
                    SELECT 'SALE'::varchar AS activity_type,
                           s.id AS resource_id,
                           s.occurred_at,
                           u.display_name AS actor_name,
                           COUNT(sl.sale_id) AS item_count,
                           COALESCE(SUM(sl.quantity), 0) AS quantity,
                           s.total_amount AS amount,
                           s.total_currency AS currency
                    FROM sale s
                    JOIN app_user u ON u.id = s.sold_by
                    LEFT JOIN sale_line sl ON sl.sale_id = s.id
                    WHERE s.total_currency = :currency
                      AND s.occurred_at <= :now
                    GROUP BY s.id, s.occurred_at, u.display_name,
                             s.total_amount, s.total_currency
                    ORDER BY s.occurred_at DESC
                    LIMIT :limit
                ),
                receipt_activities AS (
                    SELECT 'STOCK_RECEIPT'::varchar AS activity_type,
                           sm.operation_id AS resource_id,
                           MAX(sm.occurred_at) AS occurred_at,
                           u.display_name AS actor_name,
                           COUNT(DISTINCT sm.product_id) AS item_count,
                           SUM(sm.quantity) AS quantity,
                           NULL::numeric AS amount,
                           NULL::varchar AS currency
                    FROM stock_movement sm
                    JOIN app_user u ON u.id = sm.performed_by
                    WHERE sm.movement_type = 'ENTRY'
                      AND sm.occurred_at <= :now
                    GROUP BY sm.operation_id, u.display_name
                    ORDER BY MAX(sm.occurred_at) DESC
                    LIMIT :limit
                ),
                transfer_activities AS (
                    SELECT 'STOCK_TRANSFER'::varchar AS activity_type,
                           sm.operation_id AS resource_id,
                           MAX(sm.occurred_at) AS occurred_at,
                           u.display_name AS actor_name,
                           COUNT(DISTINCT sm.product_id) AS item_count,
                           SUM(sm.quantity) AS quantity,
                           NULL::numeric AS amount,
                           NULL::varchar AS currency
                    FROM stock_movement sm
                    JOIN app_user u ON u.id = sm.performed_by
                    WHERE sm.movement_type = 'TRANSFER'
                      AND sm.occurred_at <= :now
                    GROUP BY sm.operation_id, u.display_name
                    ORDER BY MAX(sm.occurred_at) DESC
                    LIMIT :limit
                ),
                payment_activities AS (
                    SELECT 'DEBT_PAYMENT'::varchar AS activity_type,
                           p.sale_id AS resource_id,
                           p.received_at AS occurred_at,
                           u.display_name AS actor_name,
                           0::bigint AS item_count,
                           0::bigint AS quantity,
                           p.amount,
                           p.currency
                    FROM payment p
                    JOIN sale s ON s.id = p.sale_id
                    JOIN app_user u ON u.id = p.received_by
                    WHERE p.currency = :currency
                      AND p.received_at > s.occurred_at
                      AND p.received_at <= :now
                    ORDER BY p.received_at DESC
                    LIMIT :limit
                ),
                combined_activity AS (
                    SELECT * FROM sale_activities
                    UNION ALL
                    SELECT * FROM receipt_activities
                    UNION ALL
                    SELECT * FROM transfer_activities
                    UNION ALL
                    SELECT * FROM payment_activities
                )
                SELECT activity_type,
                       resource_id,
                       occurred_at,
                       actor_name,
                       item_count,
                       quantity,
                       amount,
                       currency
                FROM combined_activity
                ORDER BY occurred_at DESC, activity_type ASC, resource_id ASC
                LIMIT :limit
                """, parameters, (resultSet, rowNumber) -> {
            BigDecimal amount = resultSet.getBigDecimal("amount");
            return new DashboardSummary.RecentActivity(
                    DashboardSummary.ActivityType.valueOf(
                            resultSet.getString("activity_type")),
                    resultSet.getObject("resource_id", UUID.class),
                    resultSet.getTimestamp("occurred_at").toLocalDateTime(),
                    resultSet.getString("actor_name"),
                    resultSet.getLong("item_count"),
                    resultSet.getLong("quantity"),
                    amount == null ? null : Money.create(amount, criteria.currency()));
        });
    }

    private record StockCounts(long outOfStock, long belowThreshold) {
    }

    private record SalesTotals(long todaySaleCount,
                               long todayItemsSold,
                               BigDecimal todayRevenue,
                               long last7SaleCount,
                               long last7ItemsSold,
                               BigDecimal last7Revenue,
                               BigDecimal previous7Revenue) {
    }

    private record DebtTotals(long openDebtCount,
                              long openCustomerCount,
                              long overdueCustomerCount,
                              BigDecimal totalOutstanding,
                              long days0To7Count,
                              BigDecimal days0To7Amount,
                              long days8To14Count,
                              BigDecimal days8To14Amount,
                              long days15To30Count,
                              BigDecimal days15To30Amount,
                              long over30Count,
                              BigDecimal over30Amount) {
    }
}
