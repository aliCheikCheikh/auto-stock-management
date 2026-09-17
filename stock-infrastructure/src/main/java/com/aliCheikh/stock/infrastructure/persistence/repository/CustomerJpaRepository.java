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
     * Searches given name, father name and canonical phone number; LIKE also supports partial
     * phone input.
     */
    @Query("""
            SELECT c FROM CustomerJpaEntity c
            WHERE LOWER(c.givenName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(c.fatherName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR c.phoneNumber LIKE CONCAT('%', :keyword, '%')
            ORDER BY c.givenName ASC
            """)
    List<CustomerJpaEntity> search(@Param("keyword") String keyword, Pageable pageable);

    /** Most recently registered customers for the initial selector state. */
    @Query("SELECT c FROM CustomerJpaEntity c ORDER BY c.createdAt DESC")
    List<CustomerJpaEntity> findMostRecent(Pageable pageable);
}
