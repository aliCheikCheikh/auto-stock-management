package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.SaleJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.SaleJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;


@Repository
public class SaleJpaRepositoryAdapter implements SaleRepository {

    private final SaleJpaRepository saleJpaRepository;
    private final SaleJpaMapper saleJpaMapper;

    public SaleJpaRepositoryAdapter(SaleJpaRepository saleJpaRepository, SaleJpaMapper saleJpaMapper) {
        this.saleJpaRepository = Objects.requireNonNull(saleJpaRepository, "saleJpaRepository cannot be null");
        this.saleJpaMapper = Objects.requireNonNull(saleJpaMapper, "saleJpaMapper cannot be null");
    }

    @Override
    public void save(Sale sale) {
        Objects.requireNonNull(sale, "sale cannot be null");
        saleJpaRepository.save(saleJpaMapper.toEntity(sale));
    }
}
