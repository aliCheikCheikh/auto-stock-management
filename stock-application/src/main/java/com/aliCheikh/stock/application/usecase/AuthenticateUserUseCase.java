package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;

import java.util.Objects;
import java.util.Optional;

public final class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final UserCredentialStore credentialStore;
    private final PasswordProtection passwordProtection;

    public AuthenticateUserUseCase(UserRepository userRepository,
                                   UserCredentialStore credentialStore,
                                   PasswordProtection passwordProtection) {
        this.userRepository = Objects.requireNonNull(userRepository, "Le registre des utilisateurs est obligatoire.");
        this.credentialStore = Objects.requireNonNull(credentialStore, "Le gestionnaire des accès est obligatoire.");
        this.passwordProtection = Objects.requireNonNull(passwordProtection, "La protection des mots de passe est obligatoire.");
    }

    public Optional<User> execute(UserEmail email, String password) {
        Objects.requireNonNull(email, "L'email est obligatoire.");
        if (password == null) {
            return Optional.empty();
        }

        Optional<User> matchingUser = userRepository.findByEmail(email)
                .filter(User::isActive)
                .filter(user -> credentialStore.findProtectedPassword(user.getId())
                        .filter(protectedPassword -> passwordProtection.matches(password, protectedPassword))
                        .isPresent());
        return matchingUser;
    }
}
