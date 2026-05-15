package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.StockMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, UUID>,
        JpaSpecificationExecutor<StockMovementJpaEntity> {
}
