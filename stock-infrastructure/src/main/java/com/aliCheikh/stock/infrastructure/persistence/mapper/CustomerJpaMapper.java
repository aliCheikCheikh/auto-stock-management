package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;
import com.aliCheikh.stock.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CustomerJpaMapper {

    public CustomerJpaEntity toEntity(Customer customer) {
        Objects.requireNonNull(customer, "customer cannot be null");

        return CustomerJpaEntity.of(customer.getCustomerId().getValue(),
                customer.getGivenName(),
                customer.getFatherName().orElse(null),
                // Persist the canonical value, not the diagnostic string representation.
                customer.getPhoneNumber().getValue(),
                customer.getEmail().orElse(null));
    }

    public Customer toDomain(CustomerJpaEntity customerJpaEntity) {
        Objects.requireNonNull(customerJpaEntity, "customerJpaEntity cannot be null");

        return Customer.create(CustomerId.of(customerJpaEntity.getId()),
                PhoneNumber.of(customerJpaEntity.getPhoneNumber()),
                customerJpaEntity.getGivenName(),
                customerJpaEntity.getFatherName(),
                customerJpaEntity.getEmail());
    }
}
