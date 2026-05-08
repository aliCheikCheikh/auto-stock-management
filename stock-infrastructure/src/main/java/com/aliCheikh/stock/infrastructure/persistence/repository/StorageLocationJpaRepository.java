package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageLocationJpaRepository extends JpaRepository<StorageLocationJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "stockLevels")
    Optional<StorageLocationJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = "stockLevels")
    List<StorageLocationJpaEntity> findByShopId(UUID shopId);

    @Override
    @EntityGraph(attributePaths = "stockLevels")
    List<StorageLocationJpaEntity> findAll();
}