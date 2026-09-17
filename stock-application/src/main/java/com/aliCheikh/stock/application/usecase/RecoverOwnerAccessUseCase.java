package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.RecoverOwnerAccessCommand;
import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.application.port.UserSessionRevoker;
import com.aliCheikh.stock.domain.exception.user.OwnerRecoveryNotAllowedException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;
import com.aliCheikh.stock.domain.service.PasswordRules;

import java.util.Objects;

public final class RecoverOwnerAccessUseCase {

    private final UserRepository userRepository;
    private final UserCredentialStore credentialStore;
    private final PasswordProtection passwordProtection;
    private final PasswordRules passwordRules;
    private final UserSessionRevoker sessionRevoker;
    private final TransactionRunner transactionRunner;

    public RecoverOwnerAccessUseCase(UserRepository userRepository,
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

    public User execute(RecoverOwnerAccessCommand command) {
        Objects.requireNonNull(command, "Recovery details are required.");
        return transactionRunner.execute(() -> recover(command));
    }

    private User recover(RecoverOwnerAccessCommand command) {
        UserEmail email = UserEmail.of(command.email());
        passwordRules.ensureAcceptable(command.newPassword());

        User owner = userRepository.findByEmail(email)
                .filter(User::isOwner)
                .orElseThrow(() -> new OwnerRecoveryNotAllowedException(email));

        owner.reactivate();
        owner.requirePasswordChange();
        credentialStore.replaceProtectedPassword(owner.getId(), passwordProtection.protect(command.newPassword()));
        userRepository.save(owner);
        sessionRevoker.revokeAll(owner.getId());
        return owner;
    }
}
