package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.Objects;

/** Saisie sensible volontairement absente de {@link #toString()}. */
public final class ChangeOwnPasswordCommand {

    private final UserId userId;
    private final String currentPassword;
    private final String newPassword;

    public ChangeOwnPasswordCommand(UserId userId, String currentPassword, String newPassword) {
        this.userId = Objects.requireNonNull(userId, "L'identifiant de l'utilisateur est obligatoire.");
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public UserId userId() {
        return userId;
    }

    public String currentPassword() {
        return currentPassword;
    }

    public String newPassword() {
        return newPassword;
    }
}
