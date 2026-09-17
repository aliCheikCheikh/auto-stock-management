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
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required.");
        this.credentialStore = Objects.requireNonNull(credentialStore, "Credential store is required.");
        this.passwordGenerator = Objects.requireNonNull(passwordGenerator, "Password generator is required.");
        this.passwordProtection = Objects.requireNonNull(passwordProtection, "Password protection is required.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "Transaction runner is required.");
    }

    public CreatedUser execute(CreateSellerCommand command) {
        Objects.requireNonNull(command, "Seller details are required.");
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
