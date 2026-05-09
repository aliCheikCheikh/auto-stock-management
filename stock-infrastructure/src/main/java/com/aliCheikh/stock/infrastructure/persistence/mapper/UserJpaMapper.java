package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.user.User;
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
                entity.getUsername(),
                entity.getRole()
        );
    }

    public UserJpaEntity toEntity(User user) {
        Objects.requireNonNull(user, "user cannot be null");

        return UserJpaEntity.of(
                user.getUserId().getValue(),
                user.getUserName(),
                user.getUserRole()
        );
    }
}
