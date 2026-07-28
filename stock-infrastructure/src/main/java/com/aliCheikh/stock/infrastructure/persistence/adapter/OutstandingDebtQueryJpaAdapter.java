package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.OutstandingDebtView;
import com.aliCheikh.stock.application.port.OutstandingDebtQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.projection.OutstandingDebtProjection;
import com.aliCheikh.stock.infrastructure.persistence.repository.OutstandingDebtJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lecture des créances par jointure directe vente ↔ client.
 *
 * <p>Projection plate typée : aucun agrégat n'est reconstruit, ce qui évite de charger les lignes
 * de vente inutiles à cet écran.</p>
 */
@Repository
public class OutstandingDebtQueryJpaAdapter implements OutstandingDebtQueryPort {

    private final OutstandingDebtJpaRepository outstandingDebtJpaRepository;

    public OutstandingDebtQueryJpaAdapter(OutstandingDebtJpaRepository outstandingDebtJpaRepository) {
        this.outstandingDebtJpaRepository = Objects.requireNonNull(
                outstandingDebtJpaRepository, "outstandingDebtJpaRepository cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutstandingDebtView> findAllOutstanding() {
        return toViews(outstandingDebtJpaRepository.findAllOutstanding());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutstandingDebtView> findOutstandingByCustomer(UUID customerId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        return toViews(outstandingDebtJpaRepository.findOutstandingByCustomer(customerId));
    }

    private static List<OutstandingDebtView> toViews(List<OutstandingDebtProjection> projections) {
        return projections.stream()
                .map(OutstandingDebtQueryJpaAdapter::toView)
                .toList();
    }

    private static OutstandingDebtView toView(OutstandingDebtProjection projection) {
        Currency currency = Currency.getInstance(projection.getCurrency());
        Money totalAmount = Money.create(projection.getTotalAmount(), currency);
        Money amountPaid = Money.create(projection.getAmountPaid(), currency);

        return new OutstandingDebtView(
                projection.getSaleId(),
                projection.getOccurredAt(),
                projection.getCustomerId(),
                projection.getCustomerGivenName(),
                projection.getCustomerFatherName(),
                projection.getCustomerPhoneNumber(),
                totalAmount,
                amountPaid,
                // Le solde reste dérivé, jamais stocké : une seule source de vérité.
                totalAmount.subtract(amountPaid));
    }
}
