package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.RegisterCustomerCommand;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.customer.DuplicateCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.DuplicatePhoneNumberException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;
import com.aliCheikh.stock.domain.model.customer.port.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RegisterCustomerUseCaseTest {

    private CustomerRepository customerRepository;
    private RegisterCustomerUseCase useCase;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override
            public <T> T execute(Supplier<T> work) {
                return work.get();
            }
        };
        useCase = new RegisterCustomerUseCase(customerRepository, transactionRunner);
    }

    @Test
    public void should_register_a_customer_with_a_normalized_phone_number() {
        given(customerRepository.existsByPhoneNumber(any())).willReturn(false);

        Customer customer = useCase.register(
                new RegisterCustomerCommand("66 12 34 56", "Ahmat", "Youssouf", null));

        assertThat(customer.getPhoneNumber().getValue()).isEqualTo("+23566123456");
        assertThat(customer.getGivenName()).isEqualTo("Ahmat");
        verify(customerRepository).save(customer);
    }

    @Test
    public void should_detect_a_duplicate_phone_number_whatever_the_input_format() {
        // La base contient déjà la forme canonique ; la saisie arrive dans un autre format.
        given(customerRepository.existsByPhoneNumber(PhoneNumber.of("+23566123456"))).willReturn(true);

        assertThatThrownBy(() -> useCase.register(
                new RegisterCustomerCommand("00235 66 12 34 56", "Ahmat", null, null)))
                .isInstanceOf(DuplicatePhoneNumberException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    public void should_reject_an_email_already_used() {
        given(customerRepository.existsByPhoneNumber(any())).willReturn(false);
        // L'agrégat normalise l'email : le contrôle porte sur la forme canonique.
        given(customerRepository.existsByEmail("ahmat@example.com")).willReturn(true);

        assertThatThrownBy(() -> useCase.register(
                new RegisterCustomerCommand("66 12 34 56", "Ahmat", null, "  Ahmat@Example.COM ")))
                .isInstanceOf(DuplicateCustomerEmailException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    public void should_not_check_email_uniqueness_when_no_email_is_provided() {
        given(customerRepository.existsByPhoneNumber(any())).willReturn(false);

        useCase.register(new RegisterCustomerCommand("66 12 34 56", "Ahmat", null, null));

        verify(customerRepository, never()).existsByEmail(any());
        verify(customerRepository).save(any());
    }
}
