package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.TemporaryPassword;
import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TemporaryPasswordGenerator;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.application.port.UserSessionRevoker;
import com.aliCheikh.stock.domain.exception.user.OwnerPasswordResetNotAllowedException;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;

import java.util.Objects;

public final class ResetSellerPasswordUseCase {

    private final UserRepository userRepository;
    private final UserCredentialStore credentialStore;
    private final TemporaryPasswordGenerator passwordGenerator;
    private final PasswordProtection passwordProtection;
    private final UserSessionRevoker sessionRevoker;
    private final TransactionRunner transactionRunner;

    public ResetSellerPasswordUseCase(UserRepository userRepository,
                                      UserCredentialStore credentialStore,
                                      TemporaryPasswordGenerator passwordGenerator,
                                      PasswordProtection passwordProtection,
                                      UserSessionRevoker sessionRevoker,
                                      TransactionRunner transactionRunner) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required.");
        this.credentialStore = Objects.requireNonNull(credentialStore, "Credential store is required.");
        this.passwordGenerator = Objects.requireNonNull(passwordGenerator, "Password generator is required.");
        this.passwordProtection = Objects.requireNonNull(passwordProtection, "Password protection is required.");
        this.sessionRevoker = Objects.requireNonNull(sessionRevoker, "Session revoker is required.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "Transaction runner is required.");
    }

    public TemporaryPassword execute(UserId userId) {
        Objects.requireNonNull(userId, "User ID is required.");
        return transactionRunner.execute(() -> reset(userId));
    }

    private TemporaryPassword reset(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isOwner()) {
            throw new OwnerPasswordResetNotAllowedException(userId);
        }
        user.requirePasswordChange();

        String temporaryPassword = passwordGenerator.generate();
        credentialStore.replaceProtectedPassword(userId, passwordProtection.protect(temporaryPassword));
        userRepository.save(user);
        sessionRevoker.revokeAll(userId);
        return new TemporaryPassword(user, temporaryPassword);
    }
}
