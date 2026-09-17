package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.projection.DebtProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Debt projections by settlement status. Queries share the projection and vary HAVING and
 * ordering. Pageable must remain unsorted because SQL owns the ordering.
 */
public interface DebtJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /**
     * The outer payment join and COALESCE retain sales without payments. The inner customer join
     * excludes sales without debtors. CAST gives nullable customer parameters an explicit
     * PostgreSQL type.
     */
    String FROM_DEBTS = """
            FROM sale s
            JOIN customer c ON c.id = s.customer_id
            LEFT JOIN payment p ON p.sale_id = s.id
            WHERE (CAST(:customerId AS uuid) IS NULL OR s.customer_id = CAST(:customerId AS uuid))
            """;

    String SELECT_DEBT = """
            SELECT s.id                       AS saleId,
                   s.occurred_at              AS occurredAt,
                   c.id                       AS customerId,
                   c.given_name               AS customerGivenName,
                   c.father_name              AS customerFatherName,
                   c.phone_number             AS customerPhoneNumber,
                   s.total_amount             AS totalAmount,
                   s.total_currency           AS currency,
                   COALESCE(SUM(p.amount), 0) AS amountPaid,
                   MAX(p.received_at)         AS lastPaymentAt
            """ + FROM_DEBTS;

    String GROUP_BY_SALE = """
            GROUP BY s.id, s.occurred_at, c.id, c.given_name, c.father_name,
                     c.phone_number, s.total_amount, s.total_currency
            """;

    /** Count only sale IDs, without the display projection. */
    String COUNT_DEBTS = "SELECT COUNT(*) FROM (SELECT s.id " + FROM_DEBTS
            + " GROUP BY s.id, s.total_amount ";

    String STILL_DUE = " HAVING COALESCE(SUM(p.amount), 0) < s.total_amount ";

    String NOTHING_DUE = " HAVING COALESCE(SUM(p.amount), 0) >= s.total_amount ";

    /** Outstanding debts ordered oldest first. */
    @Query(value = SELECT_DEBT + GROUP_BY_SALE + STILL_DUE + " ORDER BY s.occurred_at ASC",
            countQuery = COUNT_DEBTS + STILL_DUE + ") AS counted",
            nativeQuery = true)
    Page<DebtProjection> findOutstanding(@Param("customerId") UUID customerId, Pageable pageable);

    /** Settled debts ordered by most recent settlement. */
    @Query(value = SELECT_DEBT + GROUP_BY_SALE + NOTHING_DUE + " ORDER BY MAX(p.received_at) DESC",
            countQuery = COUNT_DEBTS + NOTHING_DUE + ") AS counted",
            nativeQuery = true)
    Page<DebtProjection> findSettled(@Param("customerId") UUID customerId, Pageable pageable);

    /** Combined history ordered by most recent activity. */
    @Query(value = SELECT_DEBT + GROUP_BY_SALE + " ORDER BY s.occurred_at DESC",
            countQuery = COUNT_DEBTS + ") AS counted",
            nativeQuery = true)
    Page<DebtProjection> findAllDebts(@Param("customerId") UUID customerId, Pageable pageable);
}
