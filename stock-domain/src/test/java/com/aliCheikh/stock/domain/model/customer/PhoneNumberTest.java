package com.aliCheikh.stock.domain.model.customer;

import com.aliCheikh.stock.domain.exception.customer.InvalidPhoneNumberException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PhoneNumberTest {

    @Test
    public void should_normalize_local_number_to_canonical_form() {
        // GIVEN un numéro local écrit avec des espaces
        // WHEN on le construit
        PhoneNumber phone = PhoneNumber.of("66 12 34 56");

        // THEN il est stocké sous forme canonique +235XXXXXXXX
        assertThat(phone.getValue()).isEqualTo("+23566123456");
    }

    @Test
    public void should_treat_different_writings_of_same_number_as_equal() {
        // GIVEN le même numéro écrit de trois façons différentes
        PhoneNumber local = PhoneNumber.of("66 12 34 56");
        PhoneNumber international = PhoneNumber.of("+235-66-12-34-56");
        PhoneNumber doubleZero = PhoneNumber.of("0023566123456");

        // THEN ils sont égaux ET partagent le même hashCode (contrat equals/hashCode)
        assertThat(local).isEqualTo(international).isEqualTo(doubleZero);
        assertThat(local.hashCode())
                .isEqualTo(international.hashCode())
                .isEqualTo(doubleZero.hashCode());
    }

    @Test
    public void should_reject_number_with_wrong_length() {
        assertThatThrownBy(() -> PhoneNumber.of("123"))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    public void should_reject_blank_number() {
        assertThatThrownBy(() -> PhoneNumber.of("   "))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }

    @Test
    public void should_reject_null_number() {
        assertThatThrownBy(() -> PhoneNumber.of(null))
                .isInstanceOf(InvalidPhoneNumberException.class);
    }
}
