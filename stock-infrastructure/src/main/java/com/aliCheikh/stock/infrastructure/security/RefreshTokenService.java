package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final RefreshTokenJpaRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    public String issue(UUID userId) {
        String rawToken = generateRawToken();
        Instant now = Instant.now();

        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.issue(
                UUID.randomUUID(),
                userId,
                sha256Hex(rawToken),
                now.plus(REFRESH_TOKEN_VALIDITY),
                now);

        repository.save(entity);

        return rawToken;
    }

    public Optional<UUID> validate(String rawToken) {
        String hashedToken = sha256Hex(rawToken);
        Instant now = Instant.now();

        return repository.findByTokenHash(hashedToken)
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt().isAfter(now))
                .map(RefreshTokenJpaEntity::getUserId);
    }

    public void revoke(String rawToken) {
        String hashedToken = sha256Hex(rawToken);
        repository.findByTokenHash(hashedToken).ifPresent(token -> {
            token.revoke();
            repository.save(token);
        });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}