package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Recherche sur le nom, le nom du père ou le numéro.
     *
     * <p>Le numéro étant stocké sous forme canonique, le LIKE permet aussi de le retrouver à
     * partir d'un fragment saisi par le vendeur.</p>
     */
    @Query("""
            SELECT c FROM CustomerJpaEntity c
            WHERE LOWER(c.givenName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(c.fatherName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR c.phoneNumber LIKE CONCAT('%', :keyword, '%')
            ORDER BY c.givenName ASC
            """)
    List<CustomerJpaEntity> search(@Param("keyword") String keyword, Pageable pageable);

    /** Les clients les plus récemment enregistrés, pour amorcer le sélecteur sans saisie. */
    @Query("SELECT c FROM CustomerJpaEntity c ORDER BY c.createdAt DESC")
    List<CustomerJpaEntity> findMostRecent(Pageable pageable);
}
