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
 * Le contrat d'entrée du résolveur, indépendamment de la base.
 *
 * <p>Ce qu'il reçoit vient d'une page de mouvements : une liste brute, avec des {@code null} pour
 * les mouvements hors vente et des doublons dès que plusieurs lignes partagent la même vente. Ces
 * deux cas sont la norme, pas l'exception, et c'est ici qu'ils sont absorbés.</p>
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

    /** Une vente réglée vaut zéro : c'est une réponse, à distinguer d'une absence de réponse. */
    @Test
    void should_report_zero_for_a_settled_sale() {
        UUID saleId = UUID.randomUUID();

        given(repository.findSettlements(anyCollection()))
                .willReturn(List.of(settlement(saleId, "50000", "50000")));

        assertThat(resolver.resolveAmountsDue(List.of(saleId)).get(saleId)).isEqualTo(xaf("0"));
    }

    /**
     * Trois lignes d'une même vente ne doivent produire qu'un identifiant interrogé : le tri
     * évite le N+1 autant que la clé dupliquée qui ferait échouer la collecte en Map.
     */
    @Test
    void should_query_each_sale_once_whatever_the_page_contains() {
        UUID saleId = UUID.randomUUID();

        given(repository.findSettlements(anyCollection()))
                .willReturn(List.of(settlement(saleId, "90000", "40000")));

        Map<UUID, Money> amountsDue = resolver.resolveAmountsDue(List.of(saleId, saleId, saleId));

        verify(repository).findSettlements(Set.of(saleId));
        assertThat(amountsDue).containsExactly(Map.entry(saleId, xaf("50000")));
    }

    /**
     * Les mouvements hors vente — réceptions, transferts — arrivent avec un identifiant nul. Les
     * laisser passer produirait un {@code IN (NULL)} inutile, et une page qui n'en contient que
     * produirait un {@code IN ()}, invalide en SQL. D'où l'absence totale d'appel.
     */
    @Test
    void should_not_query_anything_when_no_movement_comes_from_a_sale() {
        List<UUID> onlyNulls = new ArrayList<>(Arrays.asList(null, null));

        assertThat(resolver.resolveAmountsDue(onlyNulls)).isEmpty();
        assertThat(resolver.resolveAmountsDue(List.of())).isEmpty();

        verify(repository, never()).findSettlements(anyCollection());
    }

    /** Une vente introuvable est simplement absente : l'appelant décide quoi en faire. */
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
