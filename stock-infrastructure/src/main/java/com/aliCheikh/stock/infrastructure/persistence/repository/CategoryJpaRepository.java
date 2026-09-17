package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    Optional<CategoryJpaEntity> findByName(String name);

    /** Case-insensitive category name uniqueness. */
    boolean existsByNameIgnoreCase(String name);

    /** Checks name uniqueness while excluding the category being renamed. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID excludedId);

    @Query("""
            SELECT category
            FROM CategoryJpaEntity category
            WHERE lower(trim(category.name)) IN :normalizedNames
            """)
    List<CategoryJpaEntity> findByNormalizedNames(@Param("normalizedNames") Set<String> normalizedNames);
}
