package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.projection.SaleSettlementProjection;
import com.aliCheikh.stock.infrastructure.persistence.repository.SaleSettlementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Resolver input contract: movement pages may contain duplicate sale IDs and null IDs for non-sale
 * movements.
 */
class SaleSettlementResolverTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    private SaleSettlementJpaRepository repository;
    private SaleSettlementResolver resolver;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SaleSettlementJpaRepository.class);
        resolver = new SaleSettlementResolver(repository);
    }

    @Test
    void should_derive_the_remaining_balance_from_the_total_and_the_payments() {
        UUID saleId = UUID.randomUUID();

        given(repository.findSettlements(anyCollection()))
                .willReturn(List.of(settlement(saleId, "200000", "100000")));

        assertThat(resolver.resolveAmountsDue(List.of(saleId)))
                .containsExactly(Map.entry(saleId, xaf("100000")));
    }

    /** A settled balance is zero, distinct from an absent balance. */
    @Test
    void should_report_zero_for_a_settled_sale() {
        UUID saleId = UUID.randomUUID();

        given(repository.findSettlements(anyCollection()))
                .willReturn(List.of(settlement(saleId, "50000", "50000")));

        assertThat(resolver.resolveAmountsDue(List.of(saleId)).get(saleId)).isEqualTo(xaf("0"));
    }

    /** Repeated sale IDs must be queried only once and must not cause duplicate map keys. */
    @Test
    void should_query_each_sale_once_whatever_the_page_contains() {
        UUID saleId = UUID.randomUUID();

        given(repository.findSettlements(anyCollection()))
                .willReturn(List.of(settlement(saleId, "90000", "40000")));

        Map<UUID, Money> amountsDue = resolver.resolveAmountsDue(List.of(saleId, saleId, saleId));

        verify(repository).findSettlements(Set.of(saleId));
        assertThat(amountsDue).containsExactly(Map.entry(saleId, xaf("50000")));
    }

    /** Filter null IDs from non-sale movements and avoid a database call when none remain. */
    @Test
    void should_not_query_anything_when_no_movement_comes_from_a_sale() {
        List<UUID> onlyNulls = new ArrayList<>(Arrays.asList(null, null));

        assertThat(resolver.resolveAmountsDue(onlyNulls)).isEmpty();
        assertThat(resolver.resolveAmountsDue(List.of())).isEmpty();

        verify(repository, never()).findSettlements(anyCollection());
    }

    /** Unknown sales are omitted from the result. */
    @Test
    void should_omit_a_sale_which_no_longer_exists() {
        given(repository.findSettlements(anyCollection())).willReturn(List.of());

        assertThat(resolver.resolveAmountsDue(List.of(UUID.randomUUID()))).isEmpty();
    }

    private static SaleSettlementProjection settlement(UUID saleId, String total, String paid) {
        return new SaleSettlementProjection() {
            @Override
            public UUID getSaleId() {
                return saleId;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal(total);
            }

            @Override
            public String getCurrency() {
                return "XAF";
            }

            @Override
            public BigDecimal getAmountPaid() {
                return new BigDecimal(paid);
            }
        };
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
