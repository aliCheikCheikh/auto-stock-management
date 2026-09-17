package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;

import java.util.Objects;

public final class GetUserUseCase {

    private final UserRepository userRepository;

    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required.");
    }

    public User execute(UserId userId) {
        Objects.requireNonNull(userId, "User ID is required.");
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
