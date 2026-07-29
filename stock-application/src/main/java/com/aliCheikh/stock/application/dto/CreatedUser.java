package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.user.User;

import java.util.Objects;

/** Résultat à remettre au patron sans jamais inclure le mot de passe dans les journaux. */
public final class CreatedUser {

    private final User user;
    private final String temporaryPassword;

    public CreatedUser(User user, String temporaryPassword) {
        this.user = Objects.requireNonNull(user, "L'utilisateur créé est obligatoire.");
        this.temporaryPassword = Objects.requireNonNull(temporaryPassword, "Le mot de passe temporaire est obligatoire.");
    }

    public User user() {
        return user;
    }

    public String temporaryPassword() {
        return temporaryPassword;
    }
}
