package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.DebtStatus;
import com.aliCheikh.stock.application.dto.DebtSummary;
import com.aliCheikh.stock.application.dto.DebtView;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.port.DebtQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.service.CreditPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * C'est ici que la politique de crédit rencontre les créances éteintes.
 *
 * <p>Le point délicat n'est pas de lire une dette, c'est de décider à quel instant on arrête de la
 * compter : une créance soldée cesse de vieillir au jour de son règlement.</p>
 */
class ListDebtsUseCaseTest {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 28, 12, 0);

    private DebtQueryPort debtQueryPort;
    private ListDebtsUseCase useCase;

    @BeforeEach
    void setUp() {
        debtQueryPort = Mockito.mock(DebtQueryPort.class);
        useCase = new ListDebtsUseCase(
                debtQueryPort,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test
    void should_age_an_open_debt_up_to_today() {
        givenDebts(debt(NOW.minusDays(12), "200000", "50000", NOW.minusDays(4)));

        DebtSummary summary = firstSummary(DebtStatus.OUTSTANDING);

        assertThat(summary.settled()).isFalse();
        assertThat(summary.amountDue()).isEqualTo(xaf("150000"));
        assertThat(summary.daysOutstanding()).isEqualTo(12);
        assertThat(summary.overdue()).isFalse();
        // La créance vit encore : elle n'a pas de date de solde, même si des versements existent.
        assertThat(summary.settledAt()).isNull();
    }

    /**
     * Le cœur de la fonctionnalité : une dette réglée en huit jours il y a six mois doit se lire
     * « réglée en 8 jours », et non « 180 jours, en retard ». Sans cela, l'historique ferait passer
     * chaque dossier ancien pour un mauvais payeur.
     */
    @Test
    void should_stop_ageing_a_debt_on_the_day_it_was_settled() {
        LocalDateTime sale = NOW.minusDays(180);
        LocalDateTime settlement = sale.plusDays(8);

        givenDebts(debt(sale, "200000", "200000", settlement));

        DebtSummary summary = firstSummary(DebtStatus.SETTLED);

        assertThat(summary.settled()).isTrue();
        assertThat(summary.settledAt()).isEqualTo(settlement);
        assertThat(summary.daysOutstanding()).isEqualTo(8);
        assertThat(summary.overdue()).isFalse();
        assertThat(summary.amountDue()).isEqualTo(xaf("0"));
    }

    /** Une dette réglée hors délai le reste : l'information survit au paiement. */
    @Test
    void should_remember_that_a_debt_was_settled_late() {
        LocalDateTime sale = NOW.minusDays(300);
        LocalDateTime settlement = sale.plusDays(CreditPolicy.OVERDUE_AFTER_DAYS + 1);

        givenDebts(debt(sale, "50000", "50000", settlement));

        DebtSummary summary = firstSummary(DebtStatus.SETTLED);

        assertThat(summary.overdue()).isTrue();
        assertThat(summary.daysOutstanding()).isEqualTo(CreditPolicy.OVERDUE_AFTER_DAYS + 1L);
    }

    /**
     * Un versement supérieur au total ne devrait pas exister — l'agrégat le refuse — mais si la
     * base en portait un, la vente resterait soldée plutôt que d'afficher une dette négative.
     */
    @Test
    void should_treat_an_overpaid_sale_as_settled() {
        LocalDateTime sale = NOW.minusDays(3);

        givenDebts(debt(sale, "50000", "60000", NOW.minusDays(1)));

        assertThat(firstSummary(DebtStatus.ALL).settled()).isTrue();
    }

    @Test
    void should_pass_the_criteria_through_and_preserve_the_page_metadata() {
        UUID customerId = UUID.randomUUID();
        given(debtQueryPort.findByQuery(any())).willReturn(new PageResult<>(
                List.of(debt(NOW.minusDays(1), "10000", "0", null)), 2, 5, 42, 9));

        PageResult<DebtSummary> result =
                useCase.execute(new ListDebtsQuery(2, 5, DebtStatus.SETTLED, customerId));

        ArgumentCaptor<ListDebtsQuery> captor = ArgumentCaptor.forClass(ListDebtsQuery.class);
        verify(debtQueryPort).findByQuery(captor.capture());

        assertThat(captor.getValue().status()).isEqualTo(DebtStatus.SETTLED);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        // Le use case enrichit le contenu, il ne repagine rien.
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(42);
        assertThat(result.totalPages()).isEqualTo(9);
    }

    @Test
    void should_reject_a_page_size_beyond_the_allowed_range() {
        assertThatThrownBy(() ->
                new ListDebtsQuery(0, ListDebtsQuery.MAX_PAGE_SIZE + 1, DebtStatus.ALL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_a_query_without_status() {
        assertThatThrownBy(() -> new ListDebtsQuery(0, 20, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    private void givenDebts(DebtView... debts) {
        given(debtQueryPort.findByQuery(any()))
                .willReturn(new PageResult<>(List.of(debts), 0, 20, debts.length, 1));
    }

    private DebtSummary firstSummary(DebtStatus status) {
        return useCase.execute(new ListDebtsQuery(0, 20, status, null)).content().get(0);
    }

    private static DebtView debt(LocalDateTime occurredAt,
                                 String total,
                                 String paid,
                                 LocalDateTime lastPaymentAt) {
        Money totalAmount = xaf(total);
        Money amountPaid = xaf(paid);

        return new DebtView(
                UUID.randomUUID(),
                occurredAt,
                UUID.randomUUID(),
                "Moussa",
                "Youssouf",
                "+23566123456",
                totalAmount,
                amountPaid,
                totalAmount.subtract(amountPaid),
                lastPaymentAt);
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), XAF);
    }
}
