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
 * Lecture du détail d'une vente à crédit, en trois requêtes délibérément séparées.
 *
 * <p>Joindre les lignes et les paiements dans un même SELECT produirait un produit cartésien :
 * trois produits et deux encaissements donneraient six lignes, et tout cumul calculé dessus serait
 * faux. Trois requêtes ciblées sur une seule vente coûtent moins qu'un bug de montant.</p>
 */
public interface CreditSaleDetailJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /**
     * La jointure sur {@code customer} est interne : une vente sans client est une vente au
     * comptant, donc pas une créance, et l'absence de résultat est la bonne réponse.
     *
     * <p>Le cumul encaissé passe par une sous-requête plutôt que par un {@code GROUP BY} : la
     * ligne d'en-tête reste unique quel que soit le nombre de paiements.</p>
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

    /** Les lignes dans l'ordre de saisie : c'est l'ordre du ticket remis au client. */
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

    /**
     * Du plus ancien au plus récent : l'acompte du jour de la vente ouvre naturellement la liste,
     * sans avoir à le distinguer des remboursements suivants.
     */
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
