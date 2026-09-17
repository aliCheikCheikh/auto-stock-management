package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.domain.exception.customer.CustomerNotFoundException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.port.CustomerRepository;

import java.util.Objects;

/** Retrieves a customer through the application layer. */
public class GetCustomerUseCase {

    private final CustomerRepository customerRepository;

    public GetCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = Objects.requireNonNull(customerRepository, "customerRepository cannot be null");
    }

    public Customer byId(CustomerId customerId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
