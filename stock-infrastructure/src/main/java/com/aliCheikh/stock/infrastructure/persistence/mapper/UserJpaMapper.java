package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserJpaMapper {

    public User toDomain(UserJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return new User(
                UserId.of(entity.getId()),
                entity.getDisplayName(),
                UserEmail.of(entity.getEmail()),
                entity.getRole(),
                entity.isActive(),
                entity.isPasswordTemporary()
        );
    }

    public UserJpaEntity toNewEntity(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        return UserJpaEntity.newAccount(
                user.getId().getValue(),
                user.getDisplayName(),
                user.getEmail().getValue(),
                user.getRole(),
                user.isActive(),
                user.isPasswordChangeRequired());
    }

    public void updateEntity(User user, UserJpaEntity entity) {
        Objects.requireNonNull(user, "user cannot be null");
        Objects.requireNonNull(entity, "entity cannot be null");
        entity.applyProfile(user.getDisplayName(), user.isActive(), user.isPasswordChangeRequired());
    }
}
