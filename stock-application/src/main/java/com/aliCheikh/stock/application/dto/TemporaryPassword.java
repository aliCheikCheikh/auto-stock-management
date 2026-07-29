package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.user.User;

import java.util.Objects;

/** Mot de passe à afficher une seule fois, volontairement absent de {@link #toString()}. */
public final class TemporaryPassword {

    private final User user;
    private final String value;

    public TemporaryPassword(User user, String value) {
        this.user = Objects.requireNonNull(user, "L'utilisateur est obligatoire.");
        this.value = Objects.requireNonNull(value, "Le mot de passe temporaire est obligatoire.");
    }

    public User user() {
        return user;
    }

    public String value() {
        return value;
    }
}
