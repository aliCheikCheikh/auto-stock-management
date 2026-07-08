package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;

import java.util.UUID;

public record UserResponse(UUID userId,
                           String email,
                           String role,
                           boolean passwordTemporary) {

    // Ne jamais exposer le passwordHash : on ne recopie que des champs sûrs.
    public static UserResponse from(UserJpaEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isPasswordTemporary());
    }
}
