package com.aliCheikh.stock.infrastructure.config;

import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TemporaryPasswordGenerator;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.application.port.UserSessionRevoker;
import com.aliCheikh.stock.application.usecase.AuthenticateUserUseCase;
import com.aliCheikh.stock.application.usecase.ChangeOwnPasswordUseCase;
import com.aliCheikh.stock.application.usecase.CreateSellerUseCase;
import com.aliCheikh.stock.application.usecase.DeactivateUserUseCase;
import com.aliCheikh.stock.application.usecase.GetUserUseCase;
import com.aliCheikh.stock.application.usecase.ListUsersUseCase;
import com.aliCheikh.stock.application.usecase.ProvisionFirstOwnerUseCase;
import com.aliCheikh.stock.application.usecase.ReactivateUserUseCase;
import com.aliCheikh.stock.application.usecase.RecoverOwnerAccessUseCase;
import com.aliCheikh.stock.application.usecase.RenameUserUseCase;
import com.aliCheikh.stock.application.usecase.ResetSellerPasswordUseCase;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;
import com.aliCheikh.stock.domain.service.OwnerContinuity;
import com.aliCheikh.stock.domain.service.PasswordRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserUseCaseConfiguration {

    @Bean
    public OwnerContinuity ownerContinuity() {
        return new OwnerContinuity();
    }

    @Bean
    public PasswordRules passwordRules() {
        return new PasswordRules();
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository) {
        return new ListUsersUseCase(userRepository);
    }

    @Bean
    public GetUserUseCase getUserUseCase(UserRepository userRepository) {
        return new GetUserUseCase(userRepository);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository userRepository,
            UserCredentialStore credentialStore,
            PasswordProtection passwordProtection) {
        return new AuthenticateUserUseCase(userRepository, credentialStore, passwordProtection);
    }

    @Bean
    public CreateSellerUseCase createSellerUseCase(
            UserRepository userRepository,
            UserCredentialStore credentialStore,
            TemporaryPasswordGenerator passwordGenerator,
            PasswordProtection passwordProtection,
            TransactionRunner transactionRunner) {
        return new CreateSellerUseCase(
                userRepository,
                credentialStore,
                passwordGenerator,
                passwordProtection,
                transactionRunner);
    }

    @Bean
    public RenameUserUseCase renameUserUseCase(
            UserRepository userRepository,
            TransactionRunner transactionRunner) {
        return new RenameUserUseCase(userRepository, transactionRunner);
    }

    @Bean
    public DeactivateUserUseCase deactivateUserUseCase(
            UserRepository userRepository,
            OwnerContinuity ownerContinuity,
            UserSessionRevoker sessionRevoker,
            TransactionRunner transactionRunner) {
        return new DeactivateUserUseCase(
                userRepository,
                ownerContinuity,
                sessionRevoker,
                transactionRunner);
    }

    @Bean
    public ReactivateUserUseCase reactivateUserUseCase(
            UserRepository userRepository,
            TransactionRunner transactionRunner) {
        return new ReactivateUserUseCase(userRepository, transactionRunner);
    }

    @Bean
    public ResetSellerPasswordUseCase resetSellerPasswordUseCase(
            UserRepository userRepository,
            UserCredentialStore credentialStore,
            TemporaryPasswordGenerator passwordGenerator,
            PasswordProtection passwordProtection,
            UserSessionRevoker sessionRevoker,
            TransactionRunner transactionRunner) {
        return new ResetSellerPasswordUseCase(
                userRepository,
                credentialStore,
                passwordGenerator,
                passwordProtection,
                sessionRevoker,
                transactionRunner);
    }

    @Bean
    public ChangeOwnPasswordUseCase changeOwnPasswordUseCase(
            UserRepository userRepository,
            UserCredentialStore credentialStore,
            PasswordProtection passwordProtection,
            PasswordRules passwordRules,
            UserSessionRevoker sessionRevoker,
            TransactionRunner transactionRunner) {
        return new ChangeOwnPasswordUseCase(
                userRepository,
                credentialStore,
                passwordProtection,
                passwordRules,
                sessionRevoker,
                transactionRunner);
    }

    @Bean
    public ProvisionFirstOwnerUseCase provisionFirstOwnerUseCase(
            UserRepository userRepository,
            UserCredentialStore credentialStore,
            PasswordProtection passwordProtection,
            PasswordRules passwordRules,
            TransactionRunner transactionRunner) {
        return new ProvisionFirstOwnerUseCase(
                userRepository,
                credentialStore,
                passwordProtection,
                passwordRules,
                transactionRunner);
    }

    @Bean
    public RecoverOwnerAccessUseCase recoverOwnerAccessUseCase(
            UserRepository userRepository,
            UserCredentialStore credentialStore,
            PasswordProtection passwordProtection,
            PasswordRules passwordRules,
            UserSessionRevoker sessionRevoker,
            TransactionRunner transactionRunner) {
        return new RecoverOwnerAccessUseCase(
                userRepository,
                credentialStore,
                passwordProtection,
                passwordRules,
                sessionRevoker,
                transactionRunner);
    }
}
