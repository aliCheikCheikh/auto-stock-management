package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.projection.SaleSettlementProjection;
import com.aliCheikh.stock.infrastructure.persistence.repository.SaleSettlementJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Résout en un seul appel le solde restant dû d'un lot de ventes.
 *
 * <p>L'historique des mouvements doit dire si une vente a été réglée ou reste à crédit. Interroger
 * chaque vente ligne par ligne produirait un N+1 sur un écran très consulté, d'où cette résolution
 * par page — le même parti que pour les noms d'auteurs.</p>
 */
@Component
public class SaleSettlementResolver {

    private final SaleSettlementJpaRepository saleSettlementJpaRepository;

    public SaleSettlementResolver(SaleSettlementJpaRepository saleSettlementJpaRepository) {
        this.saleSettlementJpaRepository = Objects.requireNonNull(
                saleSettlementJpaRepository, "saleSettlementJpaRepository cannot be null");
    }

    /**
     * @param saleIds identifiants, doublons et {@code null} tolérés
     * @return le solde restant dû par vente ; une vente entièrement réglée vaut zéro, une vente
     * inconnue est simplement absente
     */
    @Transactional(readOnly = true)
    public Map<UUID, Money> resolveAmountsDue(Collection<UUID> saleIds) {
        Objects.requireNonNull(saleIds, "saleIds cannot be null");

        Set<UUID> distinctIds = saleIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (distinctIds.isEmpty()) {
            return Map.of();
        }

        return saleSettlementJpaRepository.findSettlements(distinctIds).stream()
                .collect(Collectors.toMap(
                        SaleSettlementProjection::getSaleId,
                        SaleSettlementResolver::toAmountDue));
    }

    private static Money toAmountDue(SaleSettlementProjection settlement) {
        Currency currency = Currency.getInstance(settlement.getCurrency());

        // Le solde reste dérivé, jamais stocké : une seule source de vérité.
        return Money.create(settlement.getTotalAmount(), currency)
                .subtract(Money.create(settlement.getAmountPaid(), currency));
    }
}
