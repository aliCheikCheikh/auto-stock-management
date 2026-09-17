package com.aliCheikh.stock.domain.model.customer;

import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerGivenNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CustomerTest {

    private static final CustomerId ID = CustomerId.generate();
    private static final PhoneNumber PHONE = PhoneNumber.of("66 12 34 56");

    @Test
    public void should_create_customer_with_all_fields() {

        Customer customer = Customer.create(ID, PHONE, "Ahmat", "Youssouf", " Ahmat@Example.COM ");


        assertThat(customer.getCustomerId()).isEqualTo(ID);
        assertThat(customer.getPhoneNumber()).isEqualTo(PHONE);
        assertThat(customer.getGivenName()).isEqualTo("Ahmat");
        assertThat(customer.getFatherName()).contains("Youssouf");
        assertThat(customer.getEmail()).contains("ahmat@example.com");
    }

    @Test
    public void should_create_customer_without_optional_fields() {

        Customer customer = Customer.create(ID, PHONE, "Ahmat", null, null);


        assertThat(customer.getGivenName()).isEqualTo("Ahmat");
        assertThat(customer.getFatherName()).isEmpty();
        assertThat(customer.getEmail()).isEmpty();
    }

    @Test
    public void should_trim_the_given_name() {
        Customer customer = Customer.create(ID, PHONE, "  Ahmat  ", null, null);

        assertThat(customer.getGivenName()).isEqualTo("Ahmat");
    }

    @Test
    public void should_reject_blank_given_name() {
        assertThatThrownBy(() -> Customer.create(ID, PHONE, "  ", "Youssouf", null))
                .isInstanceOf(InvalidCustomerGivenNameException.class);
    }

    @Test
    public void should_reject_null_given_name() {
        assertThatThrownBy(() -> Customer.create(ID, PHONE, null, "Youssouf", null))
                .isInstanceOf(InvalidCustomerGivenNameException.class);
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

        Customer a = Customer.create(ID, PHONE, "Ahmat", null, null);
        Customer b = Customer.create(ID, PhoneNumber.of("99 99 99 99"), "Autre", "Nom", null);

        // Equality uses CustomerId rather than customer details.
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
