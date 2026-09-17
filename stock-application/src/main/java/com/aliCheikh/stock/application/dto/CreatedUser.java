package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.user.User;

import java.util.Objects;

/** Result returned to the owner. Never log the temporary password. */
public final class CreatedUser {

    private final User user;
    private final String temporaryPassword;

    public CreatedUser(User user, String temporaryPassword) {
        this.user = Objects.requireNonNull(user, "Created user is required.");
        this.temporaryPassword = Objects.requireNonNull(temporaryPassword, "Temporary password is required.");
    }

    public User user() {
        return user;
    }

    public String temporaryPassword() {
        return temporaryPassword;
    }
}
