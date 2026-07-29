package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserJpaEntity user where user.role = 'OWNER' and user.active = true")
    List<UserJpaEntity> findActiveOwnersForUpdate();
}
