package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ChangeOwnPasswordCommand;
import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.application.port.UserSessionRevoker;
import com.aliCheikh.stock.domain.exception.user.IncorrectCurrentPasswordException;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;
import com.aliCheikh.stock.domain.service.PasswordRules;

import java.util.Objects;

public final class ChangeOwnPasswordUseCase {

    private final UserRepository userRepository;
    private final UserCredentialStore credentialStore;
    private final PasswordProtection passwordProtection;
    private final PasswordRules passwordRules;
    private final UserSessionRevoker sessionRevoker;
    private final TransactionRunner transactionRunner;

    public ChangeOwnPasswordUseCase(UserRepository userRepository,
                                    UserCredentialStore credentialStore,
                                    PasswordProtection passwordProtection,
                                    PasswordRules passwordRules,
                                    UserSessionRevoker sessionRevoker,
                                    TransactionRunner transactionRunner) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required.");
        this.credentialStore = Objects.requireNonNull(credentialStore, "Credential store is required.");
        this.passwordProtection = Objects.requireNonNull(passwordProtection, "Password protection is required.");
        this.passwordRules = Objects.requireNonNull(passwordRules, "Password rules are required.");
        this.sessionRevoker = Objects.requireNonNull(sessionRevoker, "Session revoker is required.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "Transaction runner is required.");
    }

    public User execute(ChangeOwnPasswordCommand command) {
        Objects.requireNonNull(command, "Password change details are required.");
        passwordRules.ensureAcceptable(command.newPassword());
        return transactionRunner.execute(() -> changePassword(command));
    }

    private User changePassword(ChangeOwnPasswordCommand command) {
        UserId userId = command.userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        String currentProtectedPassword = credentialStore.findProtectedPassword(userId)
                .orElseThrow(IncorrectCurrentPasswordException::new);
        if (!passwordProtection.matches(command.currentPassword(), currentProtectedPassword)) {
            throw new IncorrectCurrentPasswordException();
        }

        credentialStore.replaceProtectedPassword(userId, passwordProtection.protect(command.newPassword()));
        user.confirmPasswordChange();
        userRepository.save(user);
        sessionRevoker.revokeAll(userId);
        return user;
    }
}
