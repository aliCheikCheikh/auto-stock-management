package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.TemporaryPassword;
import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TemporaryPasswordGenerator;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.application.port.UserSessionRevoker;
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
        this.userRepository = Objects.requireNonNull(userRepository, "Le registre des utilisateurs est obligatoire.");
        this.credentialStore = Objects.requireNonNull(credentialStore, "Le gestionnaire des accès est obligatoire.");
        this.passwordGenerator = Objects.requireNonNull(passwordGenerator, "Le générateur de mot de passe est obligatoire.");
        this.passwordProtection = Objects.requireNonNull(passwordProtection, "La protection des mots de passe est obligatoire.");
        this.sessionRevoker = Objects.requireNonNull(sessionRevoker, "La fermeture des sessions est obligatoire.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "La transaction est obligatoire.");
    }

    public TemporaryPassword execute(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant de l'utilisateur est obligatoire.");
        return transactionRunner.execute(() -> reset(userId));
    }

    private TemporaryPassword reset(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.requirePasswordChange();

        String temporaryPassword = passwordGenerator.generate();
        credentialStore.replaceProtectedPassword(userId, passwordProtection.protect(temporaryPassword));
        userRepository.save(user);
        sessionRevoker.revokeAll(userId);
        return new TemporaryPassword(user, temporaryPassword);
    }
}
