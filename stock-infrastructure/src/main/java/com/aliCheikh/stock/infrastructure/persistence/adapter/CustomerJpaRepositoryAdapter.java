package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.exception.customer.DuplicateCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.DuplicatePhoneNumberException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;
import com.aliCheikh.stock.domain.model.customer.port.CustomerRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.CustomerJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.CustomerJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class CustomerJpaRepositoryAdapter implements CustomerRepository {
    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerJpaMapper customerJpaMapper;

    public CustomerJpaRepositoryAdapter(CustomerJpaRepository customerJpaRepository,
                                        CustomerJpaMapper customerJpaMapper) {
        this.customerJpaRepository = Objects.requireNonNull(customerJpaRepository, "customerJpaRepository cannot be null");
        this.customerJpaMapper = Objects.requireNonNull(customerJpaMapper, "customerJpaMapper cannot be null");
    }

    /**
     * Translates database uniqueness violations into domain conflicts, including concurrent
     * customer registrations.
     */
    @Override
    public void save(Customer customer) {
        Objects.requireNonNull(customer, "customer cannot be null");

        try {
            customerJpaRepository.saveAndFlush(customerJpaMapper.toEntity(customer));
        } catch (DataIntegrityViolationException violation) {
            throw translate(violation, customer);
        }
    }

    private RuntimeException translate(DataIntegrityViolationException violation, Customer customer) {
        String cause = String.valueOf(violation.getMostSpecificCause().getMessage()).toLowerCase();

        if (cause.contains("phone_number")) {
            return new DuplicatePhoneNumberException(customer.getPhoneNumber());
        }
        if (cause.contains("email")) {
            return new DuplicateCustomerEmailException(customer.getEmail().orElse(null));
        }
        return violation;
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        return customerJpaRepository.findById(customerId.getValue())
                .map(this.customerJpaMapper::toDomain);
    }

    @Override
    public boolean existsByPhoneNumber(PhoneNumber phoneNumber) {
        Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
        return customerJpaRepository.existsByPhoneNumber(phoneNumber.getValue());
    }

    @Override
    public boolean existsByEmail(String email) {
        Objects.requireNonNull(email, "email cannot be null");
        return customerJpaRepository.existsByEmail(email);
    }
}
