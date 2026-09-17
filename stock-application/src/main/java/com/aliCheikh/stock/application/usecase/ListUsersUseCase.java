package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required.");
    }

    public List<User> execute() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
