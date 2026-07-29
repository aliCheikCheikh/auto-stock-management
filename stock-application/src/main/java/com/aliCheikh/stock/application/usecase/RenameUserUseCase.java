package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;

import java.util.Objects;

public final class RenameUserUseCase {

    private final UserRepository userRepository;
    private final TransactionRunner transactionRunner;

    public RenameUserUseCase(UserRepository userRepository, TransactionRunner transactionRunner) {
        this.userRepository = Objects.requireNonNull(userRepository, "Le registre des utilisateurs est obligatoire.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "La transaction est obligatoire.");
    }

    public User execute(UserId userId, String displayName) {
        Objects.requireNonNull(userId, "L'identifiant de l'utilisateur est obligatoire.");
        return transactionRunner.execute(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
            user.rename(displayName);
            userRepository.save(user);
            return user;
        });
    }
}
