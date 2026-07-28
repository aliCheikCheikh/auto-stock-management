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
}
