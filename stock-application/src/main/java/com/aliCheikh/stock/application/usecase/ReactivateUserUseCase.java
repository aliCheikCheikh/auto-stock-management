package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;

import java.util.Objects;

public final class ReactivateUserUseCase {

    private final UserRepository userRepository;
    private final TransactionRunner transactionRunner;

    public ReactivateUserUseCase(UserRepository userRepository, TransactionRunner transactionRunner) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "Transaction runner is required.");
    }

    public User execute(UserId userId) {
        Objects.requireNonNull(userId, "User ID is required.");
        return transactionRunner.execute(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
            user.reactivate();
            userRepository.save(user);
            return user;
        });
    }
}
