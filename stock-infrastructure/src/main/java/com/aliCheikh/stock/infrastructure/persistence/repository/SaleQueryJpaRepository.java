package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SaleQueryJpaRepository extends JpaRepository<SaleJpaEntity, UUID>,
        JpaSpecificationExecutor<SaleJpaEntity> {
}
