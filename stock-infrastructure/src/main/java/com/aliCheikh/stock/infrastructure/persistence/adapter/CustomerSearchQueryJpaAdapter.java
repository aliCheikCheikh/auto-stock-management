package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.CustomerSearchView;
import com.aliCheikh.stock.application.port.CustomerSearchQueryPort;
import com.aliCheikh.stock.infrastructure.persistence.entity.CustomerJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CustomerJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Repository
public class CustomerSearchQueryJpaAdapter implements CustomerSearchQueryPort {

    private final CustomerJpaRepository customerJpaRepository;

    public CustomerSearchQueryJpaAdapter(CustomerJpaRepository customerJpaRepository) {
        this.customerJpaRepository = Objects.requireNonNull(
                customerJpaRepository, "customerJpaRepository cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSearchView> findCustomersByKeyword(String keyword, int limit) {
        Objects.requireNonNull(keyword, "keyword cannot be null");

        return customerJpaRepository.search(keyword, PageRequest.of(0, limit)).stream()
                .map(CustomerSearchQueryJpaAdapter::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSearchView> findMostRecent(int limit) {
        return customerJpaRepository.findMostRecent(PageRequest.of(0, limit)).stream()
                .map(CustomerSearchQueryJpaAdapter::toView)
                .toList();
    }

    private static CustomerSearchView toView(CustomerJpaEntity entity) {
        return new CustomerSearchView(
                entity.getId(),
                entity.getGivenName(),
                entity.getFatherName(),
                entity.getPhoneNumber());
    }
}
