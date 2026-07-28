package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.OutstandingDebtView;
import com.aliCheikh.stock.application.port.OutstandingDebtQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lecture des créances par jointure directe vente ↔ client.
 *
 * <p>Requête native et projection plate : on ne reconstruit aucun agrégat, ce qui évite le N+1
 * qu'entraînerait le chargement des lignes de vente — inutiles pour cet écran.</p>
 */
@Repository
public class OutstandingDebtQueryJpaAdapter implements OutstandingDebtQueryPort {

    private static final String BASE_QUERY = """
            SELECT s.id,
                   s.occurred_at,
                   c.id,
                   c.given_name,
                   c.father_name,
                   c.phone_number,
                   s.total_amount,
                   s.total_currency,
                   s.amount_paid
            FROM sale s
            JOIN customer c ON c.id = s.customer_id
            WHERE s.amount_paid < s.total_amount
            """;

    private final EntityManager entityManager;

    public OutstandingDebtQueryJpaAdapter(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutstandingDebtView> findAllOutstanding() {
        Query query = entityManager.createNativeQuery(BASE_QUERY + " ORDER BY s.occurred_at ASC");
        return toViews(query.getResultList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutstandingDebtView> findOutstandingByCustomer(UUID customerId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");

        Query query = entityManager
                .createNativeQuery(BASE_QUERY + " AND s.customer_id = :customerId ORDER BY s.occurred_at ASC")
                .setParameter("customerId", customerId);

        return toViews(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    private List<OutstandingDebtView> toViews(List<?> rows) {
        return ((List<Object[]>) rows).stream()
                .map(this::toView)
                .toList();
    }

    private OutstandingDebtView toView(Object[] row) {
        Currency currency = Currency.getInstance((String) row[7]);
        Money totalAmount = Money.create((BigDecimal) row[6], currency);
        Money amountPaid = Money.create((BigDecimal) row[8], currency);

        return new OutstandingDebtView(
                (UUID) row[0],
                ((Timestamp) row[1]).toLocalDateTime(),
                (UUID) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[5],
                totalAmount,
                amountPaid,
                // Le solde reste dérivé, jamais stocké : une seule source de vérité.
                totalAmount.subtract(amountPaid));
    }
}
