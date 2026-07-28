package com.aliCheikh.stock.domain.model.customer.port;

import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;

import java.util.Optional;

public interface CustomerRepository {
    void save(Customer customer);

    Optional<Customer> findById(CustomerId customerId);

    boolean existsByPhoneNumber(PhoneNumber phoneNumber);

    boolean existsByEmail(String email);

}
