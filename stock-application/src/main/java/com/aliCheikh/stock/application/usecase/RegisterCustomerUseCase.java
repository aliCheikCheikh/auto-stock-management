package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.RegisterCustomerCommand;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.customer.DuplicateCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.DuplicatePhoneNumberException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;
import com.aliCheikh.stock.domain.model.customer.port.CustomerRepository;

import java.util.Objects;

/**
 * Registers a customer after checking normalized phone and email uniqueness. Database constraints
 * protect against concurrent duplicates.
 */
public class RegisterCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final TransactionRunner transactionRunner;

    public RegisterCustomerUseCase(CustomerRepository customerRepository,
                                   TransactionRunner transactionRunner) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
    }

    public Customer register(RegisterCustomerCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        return transactionRunner.execute(() -> doRegister(command));
    }

    private Customer doRegister(RegisterCustomerCommand command) {
        // Normalize before comparing different representations of the same phone number.
        PhoneNumber phoneNumber = PhoneNumber.of(command.rawPhoneNumber());

        if (customerRepository.existsByPhoneNumber(phoneNumber)) {
            throw new DuplicatePhoneNumberException(phoneNumber);
        }

        Customer customer = Customer.create(
                CustomerId.generate(),
                phoneNumber,
                command.givenName(),
                command.fatherName(),
                command.email());

        // Check the canonical email produced by the aggregate.
        customer.getEmail().ifPresent(email -> {
            if (customerRepository.existsByEmail(email)) {
                throw new DuplicateCustomerEmailException(email);
            }
        });

        customerRepository.save(customer);
        return customer;
    }
}
