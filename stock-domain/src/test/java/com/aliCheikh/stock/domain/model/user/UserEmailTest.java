package com.aliCheikh.stock.domain.model.user;

import com.aliCheikh.stock.domain.exception.user.InvalidUserEmailException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserEmailTest {

    @Test
    void email_is_normalized_for_unambiguous_identity() {
        assertThat(UserEmail.of("  OWNER@EXAMPLE.COM ").getValue()).isEqualTo("owner@example.com");
    }

    @Test
    void malformed_email_is_rejected() {
        assertThatThrownBy(() -> UserEmail.of("owner-at-example"))
                .isInstanceOf(InvalidUserEmailException.class);
    }
}
