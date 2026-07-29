package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.application.port.UserSessionRevoker;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class RefreshTokenSessionRevoker implements UserSessionRevoker {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenSessionRevoker(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = Objects.requireNonNull(
                refreshTokenJpaRepository, "refreshTokenJpaRepository cannot be null");
    }

    @Override
    public void revokeAll(UserId userId) {
        refreshTokenJpaRepository.revokeAllActiveByUserId(userId.getValue());
    }
}
