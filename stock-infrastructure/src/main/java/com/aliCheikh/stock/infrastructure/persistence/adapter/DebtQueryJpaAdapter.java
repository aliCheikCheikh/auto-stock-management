package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.DebtView;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.port.DebtQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.projection.DebtProjection;
import com.aliCheikh.stock.infrastructure.persistence.repository.DebtJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Objects;

/** Reads debts through typed sale/customer projections without loading unused sale lines. */
@Repository
public class DebtQueryJpaAdapter implements DebtQueryPort {

    private final DebtJpaRepository debtJpaRepository;

    public DebtQueryJpaAdapter(DebtJpaRepository debtJpaRepository) {
        this.debtJpaRepository = Objects.requireNonNull(
                debtJpaRepository, "debtJpaRepository cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DebtView> findByQuery(ListDebtsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        // Keep Pageable unsorted: each query defines its own ORDER BY.
        Pageable pageable = PageRequest.of(query.page(), query.size());
        Page<DebtProjection> page = findPage(query, pageable);

        return new PageResult<>(
                page.getContent().stream().map(DebtQueryJpaAdapter::toView).toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private Page<DebtProjection> findPage(ListDebtsQuery query, Pageable pageable) {
        // An exhaustive switch requires a query for every status.
        return switch (query.status()) {
            case OUTSTANDING -> debtJpaRepository.findOutstanding(query.customerId(), pageable);
            case SETTLED -> debtJpaRepository.findSettled(query.customerId(), pageable);
            case ALL -> debtJpaRepository.findAllDebts(query.customerId(), pageable);
        };
    }

    private static DebtView toView(DebtProjection projection) {
        Currency currency = Currency.getInstance(projection.getCurrency());
        Money totalAmount = Money.create(projection.getTotalAmount(), currency);
        Money amountPaid = Money.create(projection.getAmountPaid(), currency);

        return new DebtView(
                projection.getSaleId(),
                projection.getOccurredAt(),
                projection.getCustomerId(),
                projection.getCustomerGivenName(),
                projection.getCustomerFatherName(),
                projection.getCustomerPhoneNumber(),
                totalAmount,
                amountPaid,
                // Derive the balance rather than storing it separately.
                totalAmount.subtract(amountPaid),
                projection.getLastPaymentAt());
    }
}
