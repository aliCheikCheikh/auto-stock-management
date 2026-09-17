package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.user.User;

import java.util.Objects;

/** Password displayed once and deliberately excluded from {@link #toString()}. */
public final class TemporaryPassword {

    private final User user;
    private final String value;

    public TemporaryPassword(User user, String value) {
        this.user = Objects.requireNonNull(user, "User is required.");
        this.value = Objects.requireNonNull(value, "Temporary password is required.");
    }

    public User user() {
        return user;
    }

    public String value() {
        return value;
    }
}
