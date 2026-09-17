package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ProvisionFirstOwnerCommand;
import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;
import com.aliCheikh.stock.domain.service.PasswordRules;

import java.util.Objects;
import java.util.Optional;

public final class ProvisionFirstOwnerUseCase {

    private final UserRepository userRepository;
    private final UserCredentialStore credentialStore;
    private final PasswordProtection passwordProtection;
    private final PasswordRules passwordRules;
    private final TransactionRunner transactionRunner;

    public ProvisionFirstOwnerUseCase(UserRepository userRepository,
                                      UserCredentialStore credentialStore,
                                      PasswordProtection passwordProtection,
                                      PasswordRules passwordRules,
                                      TransactionRunner transactionRunner) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required.");
        this.credentialStore = Objects.requireNonNull(credentialStore, "Credential store is required.");
        this.passwordProtection = Objects.requireNonNull(passwordProtection, "Password protection is required.");
        this.passwordRules = Objects.requireNonNull(passwordRules, "Password rules are required.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "Transaction runner is required.");
    }

    public Optional<User> execute(ProvisionFirstOwnerCommand command) {
        Objects.requireNonNull(command, "Initial owner details are required.");
        return transactionRunner.execute(() -> provision(command));
    }

    private Optional<User> provision(ProvisionFirstOwnerCommand command) {
        if (!userRepository.findAll().isEmpty()) {
            return Optional.empty();
        }

        passwordRules.ensureAcceptable(command.password());
        User owner = User.newOwner(command.displayName(), UserEmail.of(command.email()));
        userRepository.save(owner);
        credentialStore.replaceProtectedPassword(owner.getId(), passwordProtection.protect(command.password()));
        return Optional.of(owner);
    }
}
