package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshTokenJpaEntity token set token.revoked = true "
            + "where token.userId = :userId and token.revoked = false")
    int revokeAllActiveByUserId(@Param("userId") UUID userId);
}
