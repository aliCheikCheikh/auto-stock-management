package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByReference(String reference);

    Page<ProductJpaEntity> findByActiveTrue(Pageable pageable);

    long countByActiveTrue();

    @Query(value = """
                        SELECT *
                        FROM product
                        WHERE active = true
                          AND (
                            f_unaccent(lower(name))      ILIKE '%' || f_unaccent(lower(:keyword)) || '%'
                            OR f_unaccent(lower(reference)) ILIKE '%' || f_unaccent(lower(:keyword)) || '%'
                            OR f_unaccent(lower(name))      % f_unaccent(lower(:keyword))
                            OR f_unaccent(lower(reference)) % f_unaccent(lower(:keyword))
                          )
                        ORDER BY GREATEST(
                            similarity(f_unaccent(lower(name)), f_unaccent(lower(:keyword))),
                            similarity(f_unaccent(lower(reference)), f_unaccent(lower(:keyword)))
                        ) DESC
                        LIMIT :limit
            """, nativeQuery = true)
    List<ProductJpaEntity> searchActiveByKeyword(@Param("keyword") String keyword,
                                                 @Param("limit") int limit);

    boolean existsByName(String name);

    /** Checks whether a category still contains products before deletion. */
    boolean existsByCategoryId(UUID categoryId);

    @Query("""
            SELECT product
            FROM ProductJpaEntity product
            WHERE lower(trim(product.reference)) IN :normalizedReferences
            """)
    List<ProductJpaEntity> findByNormalizedReferences(
            @Param("normalizedReferences") Set<String> normalizedReferences
    );

    @Query("""
            SELECT product
            FROM ProductJpaEntity product
            WHERE lower(trim(product.name)) IN :normalizedNames
            """)
    List<ProductJpaEntity> findByNormalizedNames(@Param("normalizedNames") Set<String> normalizedNames);
}
