package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SaleQueryJpaRepository
        extends JpaRepository<SaleJpaEntity, UUID>, JpaSpecificationExecutor<SaleJpaEntity> {

    @Override
    @EntityGraph(attributePaths = "saleLines")
    Page<SaleJpaEntity> findAll(Specification<SaleJpaEntity> spec, Pageable pageable);
}