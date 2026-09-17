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

/** Resolves balances for a batch of sales to avoid one query per movement-history row. */
@Component
public class SaleSettlementResolver {

    private final SaleSettlementJpaRepository saleSettlementJpaRepository;

    public SaleSettlementResolver(SaleSettlementJpaRepository saleSettlementJpaRepository) {
        this.saleSettlementJpaRepository = Objects.requireNonNull(
                saleSettlementJpaRepository, "saleSettlementJpaRepository cannot be null");
    }

    /**
     * Accepts duplicate and null IDs. Returns balances by sale ID; settled sales have zero balance
     * and unknown sales are omitted.
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

        // Derive the balance rather than storing it separately.
        return Money.create(settlement.getTotalAmount(), currency)
                .subtract(Money.create(settlement.getAmountPaid(), currency));
    }
}
