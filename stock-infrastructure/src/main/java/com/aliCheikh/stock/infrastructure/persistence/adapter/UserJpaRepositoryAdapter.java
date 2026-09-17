package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.exception.user.DuplicateUserEmailException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.UserJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserJpaRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserJpaMapper userJpaMapper;

    public UserJpaRepositoryAdapter(UserJpaRepository userJpaRepository, UserJpaMapper userJpaMapper) {
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository, "userJpaRepository cannot be null");
        this.userJpaMapper = Objects.requireNonNull(userJpaMapper, "userJpaMapper cannot be null");
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream().map(userJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<User> findById(UserId userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        return userJpaRepository.findById(userId.getValue()).map(userJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(UserEmail email) {
        Objects.requireNonNull(email, "email cannot be null");
        return userJpaRepository.findByEmail(email.getValue()).map(userJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(UserEmail email) {
        Objects.requireNonNull(email, "email cannot be null");
        return userJpaRepository.existsByEmail(email.getValue());
    }

    @Override
    public List<User> findActiveOwners() {
        // The adapter owns locking; the domain requires a consistent view of active owners.
        return userJpaRepository.findActiveOwnersForUpdate().stream()
                .map(userJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void save(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        UserJpaEntity entity = userJpaRepository.findById(user.getId().getValue())
                .orElseGet(() -> userJpaMapper.toNewEntity(user));
        userJpaMapper.updateEntity(user, entity);

        try {
            userJpaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException violation) {
            String cause = String.valueOf(violation.getMostSpecificCause().getMessage()).toLowerCase();
            if (cause.contains("email") || cause.contains("username")) {
                throw new DuplicateUserEmailException(user.getEmail());
            }
            throw violation;
        }
    }
}
