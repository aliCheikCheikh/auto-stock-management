package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.user.InvalidPasswordException;

/** Shared validation rules for user-chosen passwords. */
public final class PasswordRules {

    public static final int MINIMUM_LENGTH = 8;
    public static final int MAXIMUM_LENGTH = 72;

    public void ensureAcceptable(String password) {
        if (password == null
                || password.length() < MINIMUM_LENGTH
                || password.length() > MAXIMUM_LENGTH) {
            throw new InvalidPasswordException(MINIMUM_LENGTH, MAXIMUM_LENGTH);
        }
    }
}
