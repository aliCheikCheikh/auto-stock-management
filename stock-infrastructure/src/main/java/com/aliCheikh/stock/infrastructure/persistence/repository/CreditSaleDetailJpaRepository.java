package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.projection.CreditSaleHeaderProjection;
import com.aliCheikh.stock.infrastructure.persistence.projection.CreditSaleLineProjection;
import com.aliCheikh.stock.infrastructure.persistence.projection.CreditSalePaymentProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads credit sale details in three queries. Joining both lines and payments would multiply rows
 * and distort payment totals.
 */
public interface CreditSaleDetailJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /**
     * The inner customer join excludes sales without a debtor. A payment subquery keeps the header
     * to one row per sale.
     */
    @Query(value = """
            SELECT s.id                AS saleId,
                   s.occurred_at       AS occurredAt,
                   s.sold_by           AS sellerId,
                   seller.display_name AS sellerName,
                   c.id                AS customerId,
                   c.given_name        AS customerGivenName,
                   c.father_name       AS customerFatherName,
                   c.phone_number      AS customerPhoneNumber,
                   s.total_amount      AS totalAmount,
                   s.total_currency    AS currency,
                   COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.sale_id = s.id), 0) AS amountPaid
            FROM sale s
            JOIN customer c ON c.id = s.customer_id
            LEFT JOIN app_user seller ON seller.id = s.sold_by
            WHERE s.id = :saleId
            """, nativeQuery = true)
    Optional<CreditSaleHeaderProjection> findHeader(@Param("saleId") UUID saleId);

    /** Sale lines in entry order. */
    @Query(value = """
            SELECT sl.product_id        AS productId,
                   pr.name              AS productName,
                   pr.reference         AS productReference,
                   sl.quantity          AS quantity,
                   sl.unit_price_amount AS unitPriceAmount,
                   sl.line_total_amount AS lineTotalAmount,
                   sl.unit_price_currency AS currency
            FROM sale_line sl
            JOIN product pr ON pr.id = sl.product_id
            WHERE sl.sale_id = :saleId
            ORDER BY sl.line_number
            """, nativeQuery = true)
    List<CreditSaleLineProjection> findLines(@Param("saleId") UUID saleId);

    /** Payments in chronological order, including the initial payment. */
    @Query(value = """
            SELECT p.id              AS paymentId,
                   p.amount          AS amount,
                   p.currency        AS currency,
                   p.received_at     AS receivedAt,
                   p.received_by     AS receivedById,
                   u.display_name    AS receivedByName
            FROM payment p
            LEFT JOIN app_user u ON u.id = p.received_by
            WHERE p.sale_id = :saleId
            ORDER BY p.received_at, p.id
            """, nativeQuery = true)
    List<CreditSalePaymentProjection> findPayments(@Param("saleId") UUID saleId);
}
