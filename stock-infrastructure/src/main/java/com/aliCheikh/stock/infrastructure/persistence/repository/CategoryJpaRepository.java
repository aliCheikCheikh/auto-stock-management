package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    Optional<CategoryJpaEntity> findByName(String name);

    /** Unicité du nom à la casse près : « Freinage » et « freinage » désignent la même famille. */
    boolean existsByNameIgnoreCase(String name);

    /** Même contrôle en excluant une catégorie : renommer sans être bloqué par soi-même. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID excludedId);
}
