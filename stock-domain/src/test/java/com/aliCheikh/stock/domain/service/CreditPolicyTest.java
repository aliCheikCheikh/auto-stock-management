package com.aliCheikh.stock.domain.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class CreditPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 28, 12, 0);

    @Test
    public void a_recent_debt_is_not_overdue() {
        assertThat(CreditPolicy.isOverdue(NOW.minusDays(5), NOW)).isFalse();
    }

    @Test
    public void a_debt_is_not_overdue_on_the_threshold_day() {
        // Exactement 30 jours : encore dans le délai toléré.
        assertThat(CreditPolicy.isOverdue(NOW.minusDays(CreditPolicy.OVERDUE_AFTER_DAYS), NOW)).isFalse();
    }

    @Test
    public void a_debt_becomes_overdue_past_the_threshold() {
        assertThat(CreditPolicy.isOverdue(NOW.minusDays(CreditPolicy.OVERDUE_AFTER_DAYS + 1), NOW)).isTrue();
    }

    @Test
    public void days_outstanding_counts_full_days_since_the_sale() {
        assertThat(CreditPolicy.daysOutstanding(NOW.minusDays(12), NOW)).isEqualTo(12);
    }

    @Test
    public void a_sale_dated_in_the_future_owes_nothing_yet() {
        // Décalage d'horloge : on ne veut pas d'ancienneté négative.
        assertThat(CreditPolicy.daysOutstanding(NOW.plusDays(3), NOW)).isZero();
    }

    @Test
    public void days_to_settle_counts_full_days_up_to_the_payment() {
        LocalDateTime sale = NOW.minusDays(23);

        assertThat(CreditPolicy.daysToSettle(sale, NOW)).isEqualTo(23);
    }

    @Test
    public void a_debt_settled_within_the_tolerance_was_not_late() {
        LocalDateTime sale = NOW.minusDays(CreditPolicy.OVERDUE_AFTER_DAYS);

        assertThat(CreditPolicy.wasSettledLate(sale, NOW)).isFalse();
    }

    @Test
    public void a_debt_settled_past_the_tolerance_was_late() {
        LocalDateTime sale = NOW.minusDays(CreditPolicy.OVERDUE_AFTER_DAYS + 1);

        assertThat(CreditPolicy.wasSettledLate(sale, NOW)).isTrue();
    }

    /**
     * Le seuil est unique : une créance jugée en retard aujourd'hui et une créance réglée après
     * la même durée relèvent du même dépassement. Deux seuils qui divergeraient un jour feraient
     * dire à l'écran des créances et à l'historique deux choses contradictoires sur la même vente.
     */
    @Test
    public void both_readings_of_the_delay_share_one_threshold() {
        LocalDateTime sale = NOW.minusDays(CreditPolicy.OVERDUE_AFTER_DAYS + 1);

        assertThat(CreditPolicy.wasSettledLate(sale, NOW))
                .isEqualTo(CreditPolicy.isOverdue(sale, NOW));
        assertThat(CreditPolicy.daysToSettle(sale, NOW))
                .isEqualTo(CreditPolicy.daysOutstanding(sale, NOW));
    }

    /** Un règlement horodaté avant la vente ne doit pas produire une durée négative. */
    @Test
    public void a_settlement_dated_before_the_sale_takes_no_time_at_all() {
        assertThat(CreditPolicy.daysToSettle(NOW, NOW.minusDays(3))).isZero();
    }
}
