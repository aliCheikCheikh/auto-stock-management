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

    /**
     * Le montant encaissé est agrégé depuis le ledger des paiements : une vente sans aucun
     * encaissement reste retenue, d'où la jointure externe et le {@code COALESCE}.
     */
    String SELECT_OUTSTANDING = """
            SELECT s.id                          AS saleId,
                   s.occurred_at                 AS occurredAt,
                   c.id                          AS customerId,
                   c.given_name                  AS customerGivenName,
                   c.father_name                 AS customerFatherName,
                   c.phone_number                AS customerPhoneNumber,
                   s.total_amount                AS totalAmount,
                   s.total_currency              AS currency,
                   COALESCE(SUM(p.amount), 0)    AS amountPaid
            FROM sale s
            JOIN customer c ON c.id = s.customer_id
            LEFT JOIN payment p ON p.sale_id = s.id
            """;

    String GROUP_AND_FILTER = """
            GROUP BY s.id, s.occurred_at, c.id, c.given_name, c.father_name,
                     c.phone_number, s.total_amount, s.total_currency
            HAVING COALESCE(SUM(p.amount), 0) < s.total_amount
            ORDER BY s.occurred_at ASC
            """;

    @Query(value = SELECT_OUTSTANDING + GROUP_AND_FILTER, nativeQuery = true)
    List<OutstandingDebtProjection> findAllOutstanding();

    @Query(value = SELECT_OUTSTANDING + " WHERE s.customer_id = :customerId " + GROUP_AND_FILTER,
            nativeQuery = true)
    List<OutstandingDebtProjection> findOutstandingByCustomer(@Param("customerId") UUID customerId);
}
