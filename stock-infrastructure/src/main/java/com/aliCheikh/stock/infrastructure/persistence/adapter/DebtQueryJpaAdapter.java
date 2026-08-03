package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.DebtView;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.port.DebtQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.projection.DebtProjection;
import com.aliCheikh.stock.infrastructure.persistence.repository.DebtJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Objects;

/**
 * Lecture des créances par jointure directe vente ↔ client.
 *
 * <p>Projection plate typée : aucun agrégat n'est reconstruit, ce qui évite de charger les lignes
 * de vente inutiles à cet écran.</p>
 */
@Repository
public class DebtQueryJpaAdapter implements DebtQueryPort {

    private final DebtJpaRepository debtJpaRepository;

    public DebtQueryJpaAdapter(DebtJpaRepository debtJpaRepository) {
        this.debtJpaRepository = Objects.requireNonNull(
                debtJpaRepository, "debtJpaRepository cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DebtView> findByQuery(ListDebtsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        // Pageable sans tri : chaque requête porte son propre ORDER BY, et Spring Data ajouterait
        // le sien à la suite s'il en recevait un.
        Pageable pageable = PageRequest.of(query.page(), query.size());
        Page<DebtProjection> page = findPage(query, pageable);

        return new PageResult<>(
                page.getContent().stream().map(DebtQueryJpaAdapter::toView).toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private Page<DebtProjection> findPage(ListDebtsQuery query, Pageable pageable) {
        // Un switch exhaustif sur l'énumération : ajouter un statut sans lui donner de requête ne
        // compilera pas, là où un if/else aurait silencieusement renvoyé la branche par défaut.
        return switch (query.status()) {
            case OUTSTANDING -> debtJpaRepository.findOutstanding(query.customerId(), pageable);
            case SETTLED -> debtJpaRepository.findSettled(query.customerId(), pageable);
            case ALL -> debtJpaRepository.findAllDebts(query.customerId(), pageable);
        };
    }

    private static DebtView toView(DebtProjection projection) {
        Currency currency = Currency.getInstance(projection.getCurrency());
        Money totalAmount = Money.create(projection.getTotalAmount(), currency);
        Money amountPaid = Money.create(projection.getAmountPaid(), currency);

        return new DebtView(
                projection.getSaleId(),
                projection.getOccurredAt(),
                projection.getCustomerId(),
                projection.getCustomerGivenName(),
                projection.getCustomerFatherName(),
                projection.getCustomerPhoneNumber(),
                totalAmount,
                amountPaid,
                // Le solde reste dérivé, jamais stocké : une seule source de vérité.
                totalAmount.subtract(amountPaid),
                projection.getLastPaymentAt());
    }
}
