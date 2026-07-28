package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.customer.CustomerId;

/** Levée quand un client référencé n'existe pas. */
public class CustomerNotFoundException extends DomainException {

    private final CustomerId customerId;

    public CustomerNotFoundException(CustomerId customerId) {
        super("Customer not found: " + customerId);
        this.customerId = customerId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }
}
