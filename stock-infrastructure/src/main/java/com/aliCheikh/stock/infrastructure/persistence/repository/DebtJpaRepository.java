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
 * Lecture des ventes à crédit, par statut de règlement.
 *
 * <p>Requête native avec alias explicites, projetée sur une interface typée. On ne charge ni les
 * agrégats ni les lignes de vente, inutiles pour cet écran.</p>
 *
 * <p>Le corps de la requête est unique : les trois statuts n'en font varier que le {@code HAVING}
 * et l'ordre. Trois requêtes recopiées auraient dérivé à la première évolution du modèle de
 * lecture — et ce sont des montants qu'elles calculent.</p>
 *
 * <p>L'ordre est écrit dans le SQL plutôt que délégué au {@code Pageable}, qui est toujours reçu
 * sans tri : Spring Data ajouterait sinon son propre {@code ORDER BY} à la suite, et un tri sur un
 * agrégat ne survit pas à cette concaténation.</p>
 */
public interface DebtJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /**
     * Le montant encaissé est agrégé depuis le ledger des paiements : une vente sans aucun
     * encaissement doit rester visible, d'où la jointure externe et le {@code COALESCE}. La
     * jointure au client, elle, est interne — une vente sans client est une vente au comptant,
     * jamais une créance.
     *
     * <p>Le client est filtré ici et non par une méthode dédiée : c'est un critère, pas un cas
     * d'usage. Le {@code CAST} est nécessaire parce qu'un paramètre nul ne porte aucun type que
     * PostgreSQL puisse deviner.</p>
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

    /** Le comptage ne retient que l'identifiant : le reste de la projection ne lui sert à rien. */
    String COUNT_DEBTS = "SELECT COUNT(*) FROM (SELECT s.id " + FROM_DEBTS
            + " GROUP BY s.id, s.total_amount ";

    String STILL_DUE = " HAVING COALESCE(SUM(p.amount), 0) < s.total_amount ";

    String NOTHING_DUE = " HAVING COALESCE(SUM(p.amount), 0) >= s.total_amount ";

    /** De la plus ancienne à la plus récente : c'est la plus ancienne qu'on relance. */
    @Query(value = SELECT_DEBT + GROUP_BY_SALE + STILL_DUE + " ORDER BY s.occurred_at ASC",
            countQuery = COUNT_DEBTS + STILL_DUE + ") AS counted",
            nativeQuery = true)
    Page<DebtProjection> findOutstanding(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Du règlement le plus récent au plus ancien : on ouvre l'historique pour retrouver ce qui
     * vient d'être soldé, pas pour remonter à la première vente de la boutique.
     */
    @Query(value = SELECT_DEBT + GROUP_BY_SALE + NOTHING_DUE + " ORDER BY MAX(p.received_at) DESC",
            countQuery = COUNT_DEBTS + NOTHING_DUE + ") AS counted",
            nativeQuery = true)
    Page<DebtProjection> findSettled(@Param("customerId") UUID customerId, Pageable pageable);

    /** Les deux mêlées, l'activité la plus récente en tête. */
    @Query(value = SELECT_DEBT + GROUP_BY_SALE + " ORDER BY s.occurred_at DESC",
            countQuery = COUNT_DEBTS + ") AS counted",
            nativeQuery = true)
    Page<DebtProjection> findAllDebts(@Param("customerId") UUID customerId, Pageable pageable);
}
