package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CreateSellerCommand;
import com.aliCheikh.stock.application.dto.CreatedUser;
import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TemporaryPasswordGenerator;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.domain.exception.user.DuplicateUserEmailException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;

import java.util.Objects;

public final class CreateSellerUseCase {

    private final UserRepository userRepository;
    private final UserCredentialStore credentialStore;
    private final TemporaryPasswordGenerator passwordGenerator;
    private final PasswordProtection passwordProtection;
    private final TransactionRunner transactionRunner;

    public CreateSellerUseCase(UserRepository userRepository,
                               UserCredentialStore credentialStore,
                               TemporaryPasswordGenerator passwordGenerator,
                               PasswordProtection passwordProtection,
                               TransactionRunner transactionRunner) {
        this.userRepository = Objects.requireNonNull(userRepository, "Le registre des utilisateurs est obligatoire.");
        this.credentialStore = Objects.requireNonNull(credentialStore, "Le gestionnaire des accès est obligatoire.");
        this.passwordGenerator = Objects.requireNonNull(passwordGenerator, "Le générateur de mot de passe est obligatoire.");
        this.passwordProtection = Objects.requireNonNull(passwordProtection, "La protection des mots de passe est obligatoire.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "La transaction est obligatoire.");
    }

    public CreatedUser execute(CreateSellerCommand command) {
        Objects.requireNonNull(command, "Les informations du vendeur sont obligatoires.");
        UserEmail email = UserEmail.of(command.email());
        User seller = User.newSeller(command.displayName(), email);

        return transactionRunner.execute(() -> createSeller(seller));
    }

    private CreatedUser createSeller(User seller) {
        if (userRepository.existsByEmail(seller.getEmail())) {
            throw new DuplicateUserEmailException(seller.getEmail());
        }

        String temporaryPassword = passwordGenerator.generate();
        String protectedPassword = passwordProtection.protect(temporaryPassword);
        userRepository.save(seller);
        credentialStore.replaceProtectedPassword(seller.getId(), protectedPassword);
        return new CreatedUser(seller, temporaryPassword);
    }
}
