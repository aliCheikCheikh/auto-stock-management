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
 * Enregistre un client à qui des ventes à crédit pourront être accordées.
 *
 * <p>L'unicité du téléphone (et de l'email s'il est renseigné) ne peut pas être portée par
 * l'agrégat, qui ne voit que lui-même : elle est vérifiée ici, via le repository, pour renvoyer une
 * erreur métier explicite. La contrainte en base reste le garde-fou ultime en cas de création
 * concurrente.</p>
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
        // Le Value Object normalise avant toute comparaison : sans cela, "66 12 34 56" et
        // "+235 66 12 34 56" échapperaient au contrôle de doublon.
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

        // L'agrégat a normalisé l'email (trim + minuscules) : on interroge la forme canonique.
        customer.getEmail().ifPresent(email -> {
            if (customerRepository.existsByEmail(email)) {
                throw new DuplicateCustomerEmailException(email);
            }
        });

        customerRepository.save(customer);
        return customer;
    }
}
