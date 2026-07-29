package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.user.InvalidPasswordException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordRulesTest {

    private final PasswordRules passwordRules = new PasswordRules();

    @Test
    void a_password_must_have_the_minimum_length() {
        assertThatThrownBy(() -> passwordRules.ensureAcceptable("court"))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void a_password_cannot_exceed_the_maximum_length() {
        assertThatThrownBy(() -> passwordRules.ensureAcceptable("a".repeat(73)))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void an_acceptable_password_is_kept_private_by_the_rule() {
        assertThatCode(() -> passwordRules.ensureAcceptable("Suffisant123!"))
                .doesNotThrowAnyException();
    }
}
