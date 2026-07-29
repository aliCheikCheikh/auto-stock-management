package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ChangeOwnPasswordCommand;
import com.aliCheikh.stock.application.dto.CreateSellerCommand;
import com.aliCheikh.stock.application.dto.CreatedUser;
import com.aliCheikh.stock.application.dto.TemporaryPassword;
import com.aliCheikh.stock.application.port.PasswordProtection;
import com.aliCheikh.stock.application.port.TemporaryPasswordGenerator;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserCredentialStore;
import com.aliCheikh.stock.application.port.UserSessionRevoker;
import com.aliCheikh.stock.domain.exception.user.DuplicateUserEmailException;
import com.aliCheikh.stock.domain.exception.user.IncorrectCurrentPasswordException;
import com.aliCheikh.stock.domain.exception.user.LastActiveOwnerException;
import com.aliCheikh.stock.domain.exception.user.OwnerPasswordResetNotAllowedException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;
import com.aliCheikh.stock.domain.service.OwnerContinuity;
import com.aliCheikh.stock.domain.service.PasswordRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserManagementUseCaseTest {

    private static final String GENERATED_PASSWORD = "Temp-4827-Stock";

    private InMemoryUsers users;
    private InMemoryCredentials credentials;
    private RecordingSessionRevoker sessionRevoker;
    private PasswordProtection passwordProtection;
    private TransactionRunner transactionRunner;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        credentials = new InMemoryCredentials();
        sessionRevoker = new RecordingSessionRevoker();
        passwordProtection = new PlainTestPasswordProtection();
        transactionRunner = new ImmediateTransactionRunner();
    }

    @Test
    void creating_a_seller_generates_and_protects_a_one_time_password() {
        CreateSellerUseCase createSeller = createSellerUseCase();

        CreatedUser created = createSeller.execute(new CreateSellerCommand(" Amina Mahamat ", " AMINA@EXAMPLE.COM "));

        assertThat(created.user().getDisplayName()).isEqualTo("Amina Mahamat");
        assertThat(created.user().getEmail().getValue()).isEqualTo("amina@example.com");
        assertThat(created.temporaryPassword()).isEqualTo(GENERATED_PASSWORD);
        assertThat(credentials.findProtectedPassword(created.user().getId()))
                .contains("protected:" + GENERATED_PASSWORD);
        assertThat(created.toString()).doesNotContain(GENERATED_PASSWORD);
    }

    @Test
    void duplicate_email_is_rejected_after_normalization() {
        users.save(User.newSeller("Existant", UserEmail.of("seller@example.com")));

        assertThatThrownBy(() -> createSellerUseCase().execute(
                new CreateSellerCommand("Nouveau", " SELLER@EXAMPLE.COM ")))
                .isInstanceOf(DuplicateUserEmailException.class);
    }

    @Test
    void the_last_active_owner_cannot_be_deactivated() {
        User owner = User.newOwner("Patron", UserEmail.of("owner@example.com"));
        users.save(owner);

        assertThatThrownBy(() -> deactivateUserUseCase().execute(owner.getId()))
                .isInstanceOf(LastActiveOwnerException.class);
        assertThat(owner.isActive()).isTrue();
        assertThat(sessionRevoker.revokedUserIds).isEmpty();
    }

    @Test
    void an_owner_can_be_deactivated_when_another_active_owner_remains() {
        User owner = User.newOwner("Patron", UserEmail.of("owner@example.com"));
        users.save(owner);
        users.save(User.newOwner("Remplaçant", UserEmail.of("backup@example.com")));

        deactivateUserUseCase().execute(owner.getId());

        assertThat(owner.isActive()).isFalse();
        assertThat(sessionRevoker.revokedUserIds).containsExactly(owner.getId());
    }

    @Test
    void resetting_a_seller_password_forces_change_and_revokes_sessions() {
        User seller = User.newSeller("Vendeur", UserEmail.of("seller@example.com"));
        seller.confirmPasswordChange();
        users.save(seller);

        TemporaryPassword temporaryPassword = resetSellerPasswordUseCase().execute(seller.getId());

        assertThat(temporaryPassword.value()).isEqualTo(GENERATED_PASSWORD);
        assertThat(temporaryPassword.toString()).doesNotContain(GENERATED_PASSWORD);
        assertThat(seller.isPasswordChangeRequired()).isTrue();
        assertThat(credentials.findProtectedPassword(seller.getId()))
                .contains("protected:" + GENERATED_PASSWORD);
        assertThat(sessionRevoker.revokedUserIds).containsExactly(seller.getId());
    }

    @Test
    void owner_password_reset_is_reserved_for_server_recovery() {
        User owner = User.newOwner("Patron", UserEmail.of("owner@example.com"));
        users.save(owner);

        assertThatThrownBy(() -> resetSellerPasswordUseCase().execute(owner.getId()))
                .isInstanceOf(OwnerPasswordResetNotAllowedException.class);
    }

    @Test
    void an_inactive_user_cannot_authenticate_with_a_valid_password() {
        User seller = User.newSeller("Vendeur", UserEmail.of("seller@example.com"));
        seller.deactivate();
        users.save(seller);
        credentials.replaceProtectedPassword(seller.getId(), "protected:Correct123!");

        Optional<User> authenticated = new AuthenticateUserUseCase(users, credentials, passwordProtection)
                .execute(seller.getEmail(), "Correct123!");

        assertThat(authenticated).isEmpty();
    }

    @Test
    void changing_password_rejects_an_incorrect_current_password() {
        User seller = establishedSeller();

        assertThatThrownBy(() -> changeOwnPasswordUseCase().execute(
                new ChangeOwnPasswordCommand(seller.getId(), "Incorrect", "Nouveau123!")))
                .isInstanceOf(IncorrectCurrentPasswordException.class);
        assertThat(credentials.findProtectedPassword(seller.getId())).contains("protected:Actuel123!");
    }

    @Test
    void changing_password_clears_the_requirement_and_revokes_other_sessions() {
        User seller = establishedSeller();
        seller.requirePasswordChange();

        changeOwnPasswordUseCase().execute(
                new ChangeOwnPasswordCommand(seller.getId(), "Actuel123!", "Nouveau123!"));

        assertThat(seller.isPasswordChangeRequired()).isFalse();
        assertThat(credentials.findProtectedPassword(seller.getId())).contains("protected:Nouveau123!");
        assertThat(sessionRevoker.revokedUserIds).containsExactly(seller.getId());
    }

    private User establishedSeller() {
        User seller = User.newSeller("Vendeur", UserEmail.of("seller@example.com"));
        seller.confirmPasswordChange();
        users.save(seller);
        credentials.replaceProtectedPassword(seller.getId(), "protected:Actuel123!");
        return seller;
    }

    private CreateSellerUseCase createSellerUseCase() {
        TemporaryPasswordGenerator generator = () -> GENERATED_PASSWORD;
        return new CreateSellerUseCase(users, credentials, generator, passwordProtection, transactionRunner);
    }

    private DeactivateUserUseCase deactivateUserUseCase() {
        return new DeactivateUserUseCase(users, new OwnerContinuity(), sessionRevoker, transactionRunner);
    }

    private ResetSellerPasswordUseCase resetSellerPasswordUseCase() {
        return new ResetSellerPasswordUseCase(
                users,
                credentials,
                () -> GENERATED_PASSWORD,
                passwordProtection,
                sessionRevoker,
                transactionRunner);
    }

    private ChangeOwnPasswordUseCase changeOwnPasswordUseCase() {
        return new ChangeOwnPasswordUseCase(
                users,
                credentials,
                passwordProtection,
                new PasswordRules(),
                sessionRevoker,
                transactionRunner);
    }

    private static final class InMemoryUsers implements UserRepository {
        private final Map<UserId, User> usersById = new LinkedHashMap<>();

        @Override
        public List<User> findAll() {
            return new ArrayList<>(usersById.values());
        }

        @Override
        public Optional<User> findById(UserId userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public Optional<User> findByEmail(UserEmail email) {
            return usersById.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(UserEmail email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public List<User> findActiveOwners() {
            return usersById.values().stream()
                    .filter(User::isOwner)
                    .filter(User::isActive)
                    .toList();
        }

        @Override
        public void save(User user) {
            usersById.put(user.getId(), user);
        }
    }

    private static final class InMemoryCredentials implements UserCredentialStore {
        private final Map<UserId, String> protectedPasswords = new LinkedHashMap<>();

        @Override
        public Optional<String> findProtectedPassword(UserId userId) {
            return Optional.ofNullable(protectedPasswords.get(userId));
        }

        @Override
        public void replaceProtectedPassword(UserId userId, String protectedPassword) {
            protectedPasswords.put(userId, protectedPassword);
        }
    }

    private static final class PlainTestPasswordProtection implements PasswordProtection {
        @Override
        public String protect(String password) {
            return "protected:" + password;
        }

        @Override
        public boolean matches(String password, String protectedPassword) {
            return protect(password).equals(protectedPassword);
        }
    }

    private static final class RecordingSessionRevoker implements UserSessionRevoker {
        private final Set<UserId> revokedUserIds = new java.util.LinkedHashSet<>();

        @Override
        public void revokeAll(UserId userId) {
            revokedUserIds.add(userId);
        }
    }

    private static final class ImmediateTransactionRunner implements TransactionRunner {
        @Override
        public <T> T execute(Supplier<T> work) {
            return work.get();
        }
    }
}
