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

    /**
     * Noms affichables d'un lot d'utilisateurs.
     *
     * <p>Résoudre l'auteur ligne par ligne dans un historique produirait un N+1 : on récupère donc
     * tous les noms d'une page en une seule requête, et seulement les deux colonnes utiles.</p>
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT u.id AS id, u.displayName AS displayName
            FROM UserJpaEntity u
            WHERE u.id IN :ids
            """)
    java.util.List<com.aliCheikh.stock.infrastructure.persistence.projection.UserDisplayNameProjection>
            findDisplayNames(@org.springframework.data.repository.query.Param("ids") java.util.Collection<java.util.UUID> ids);
}
