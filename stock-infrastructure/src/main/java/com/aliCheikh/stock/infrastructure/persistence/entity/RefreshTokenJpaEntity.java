package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshTokenJpaEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshTokenJpaEntity() {
    }

    private RefreshTokenJpaEntity(UUID id, UUID userId, String tokenHash,
                                  Instant expiresAt, boolean revoked, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.revoked = revoked;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static RefreshTokenJpaEntity issue(UUID id, UUID userId, String tokenHash,
                                              Instant expiresAt, Instant createdAt) {
        return new RefreshTokenJpaEntity(id, userId, tokenHash, expiresAt, false, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void revoke() {
        this.revoked = true;
    }
}

