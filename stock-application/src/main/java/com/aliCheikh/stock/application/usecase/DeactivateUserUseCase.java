package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.application.port.UserSessionRevoker;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.domain.model.user.port.UserRepository;
import com.aliCheikh.stock.domain.service.OwnerContinuity;

import java.util.Objects;

public final class DeactivateUserUseCase {

    private final UserRepository userRepository;
    private final OwnerContinuity ownerContinuity;
    private final UserSessionRevoker sessionRevoker;
    private final TransactionRunner transactionRunner;

    public DeactivateUserUseCase(UserRepository userRepository,
                                 OwnerContinuity ownerContinuity,
                                 UserSessionRevoker sessionRevoker,
                                 TransactionRunner transactionRunner) {
        this.userRepository = Objects.requireNonNull(userRepository, "Le registre des utilisateurs est obligatoire.");
        this.ownerContinuity = Objects.requireNonNull(ownerContinuity, "La continuité du propriétaire est obligatoire.");
        this.sessionRevoker = Objects.requireNonNull(sessionRevoker, "La fermeture des sessions est obligatoire.");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "La transaction est obligatoire.");
    }

    public User execute(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant de l'utilisateur est obligatoire.");
        return transactionRunner.execute(() -> deactivate(userId));
    }

    private User deactivate(UserId userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        long activeOwnerCount = user.isOwner() ? userRepository.findActiveOwners().size() : 0;
        ownerContinuity.ensureDeactivationKeepsAnActiveOwner(user, activeOwnerCount);

        user.deactivate();
        userRepository.save(user);
        sessionRevoker.revokeAll(userId);
        return user;
    }
}
