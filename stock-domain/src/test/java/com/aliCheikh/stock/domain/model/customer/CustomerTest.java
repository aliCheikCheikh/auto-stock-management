package com.aliCheikh.stock.domain.model.customer;

import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerLastNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CustomerTest {

    private static final CustomerId ID = CustomerId.generate();
    private static final PhoneNumber PHONE = PhoneNumber.of("66 12 34 56");

    @Test
    public void should_create_customer_with_all_fields() {
        // WHEN on crée un client complet (email en casse mixte, avec espaces)
        Customer customer = Customer.create(ID, PHONE, "Ahmat", "Youssouf", " Ahmat@Example.COM ");

        // THEN les champs sont posés et l'email est normalisé (trim + minuscules)
        assertThat(customer.getCustomerId()).isEqualTo(ID);
        assertThat(customer.getPhoneNumber()).isEqualTo(PHONE);
        assertThat(customer.getLastName()).isEqualTo("Ahmat");
        assertThat(customer.getFirstName()).contains("Youssouf");
        assertThat(customer.getEmail()).contains("ahmat@example.com");
    }

    @Test
    public void should_create_customer_without_optional_fields() {
        // WHEN prénom et email absents (cas courant au Tchad)
        Customer customer = Customer.create(ID, PHONE, "Ahmat", null, null);

        // THEN les optionnels sont vides, pas d'exception
        assertThat(customer.getFirstName()).isEmpty();
        assertThat(customer.getEmail()).isEmpty();
    }

    @Test
    public void should_reject_blank_last_name() {
        assertThatThrownBy(() -> Customer.create(ID, PHONE, "  ", "Youssouf", null))
                .isInstanceOf(InvalidCustomerLastNameException.class);
    }

    @Test
    public void should_require_phone_number() {
        assertThatNullPointerException()
                .isThrownBy(() -> Customer.create(ID, null, "Ahmat", null, null));
    }

    @Test
    public void should_reject_invalid_email_when_present() {
        assertThatThrownBy(() -> Customer.create(ID, PHONE, "Ahmat", null, "pas-un-email"))
                .isInstanceOf(InvalidCustomerEmailException.class);
    }

    @Test
    public void two_customers_with_same_id_are_equal() {
        // GIVEN deux clients partageant le même identifiant technique
        Customer a = Customer.create(ID, PHONE, "Ahmat", null, null);
        Customer b = Customer.create(ID, PhoneNumber.of("99 99 99 99"), "Autre", "Nom", null);

        // THEN ils sont égaux : l'identité passe par le CustomerId, pas par les données
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
