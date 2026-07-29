package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class UserCredentialJpaStore implements UserCredentialStore {

    private final UserJpaRepository userJpaRepository;

    public UserCredentialJpaStore(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository, "userJpaRepository cannot be null");
    }

    @Override
    public Optional<String> findProtectedPassword(UserId userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return userJpaRepository.findById(userId.getValue()).map(UserJpaEntity::getPasswordHash);
    }

    @Override
    public void replaceProtectedPassword(UserId userId, String protectedPassword) {
        Objects.requireNonNull(userId, "userId cannot be null");
        UserJpaEntity user = userJpaRepository.findById(userId.getValue())
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.replaceProtectedPassword(protectedPassword);
        userJpaRepository.saveAndFlush(user);
    }
}
