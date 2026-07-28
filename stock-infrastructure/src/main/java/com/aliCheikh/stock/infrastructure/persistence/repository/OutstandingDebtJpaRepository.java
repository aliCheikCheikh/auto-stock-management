package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.projection.OutstandingDebtProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Lecture des créances : les ventes dont le montant encaissé n'atteint pas le total.
 *
 * <p>Requête native avec alias explicites, projetée sur une interface typée. On ne charge pas les
 * agrégats ni les lignes de vente, inutiles pour cet écran.</p>
 */
public interface OutstandingDebtJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    String SELECT_OUTSTANDING = """
            SELECT s.id                AS saleId,
                   s.occurred_at       AS occurredAt,
                   c.id                AS customerId,
                   c.given_name        AS customerGivenName,
                   c.father_name       AS customerFatherName,
                   c.phone_number      AS customerPhoneNumber,
                   s.total_amount      AS totalAmount,
                   s.total_currency    AS currency,
                   s.amount_paid       AS amountPaid
            FROM sale s
            JOIN customer c ON c.id = s.customer_id
            WHERE s.amount_paid < s.total_amount
            """;

    @Query(value = SELECT_OUTSTANDING + " ORDER BY s.occurred_at ASC", nativeQuery = true)
    List<OutstandingDebtProjection> findAllOutstanding();

    @Query(value = SELECT_OUTSTANDING + " AND s.customer_id = :customerId ORDER BY s.occurred_at ASC",
            nativeQuery = true)
    List<OutstandingDebtProjection> findOutstandingByCustomer(@Param("customerId") UUID customerId);
}
