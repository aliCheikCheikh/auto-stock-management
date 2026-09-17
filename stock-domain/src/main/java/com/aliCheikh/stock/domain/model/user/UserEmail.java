package com.aliCheikh.stock.domain.model.user;

import com.aliCheikh.stock.domain.exception.user.InvalidUserEmailException;

import java.util.Objects;
import java.util.regex.Pattern;

/** Email address used to sign in. */
public final class UserEmail {

    public static final int MAX_LENGTH = 255;

    private static final Pattern VALID_FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String value;

    private UserEmail(String value) {
        this.value = normalizeAndValidate(value);
    }

    public static UserEmail of(String value) {
        return new UserEmail(value);
    }

    private String normalizeAndValidate(String rawValue) {
        if (rawValue == null) {
            throw new InvalidUserEmailException(null);
        }
        String normalizedValue = rawValue.trim().toLowerCase();
        if (normalizedValue.length() > MAX_LENGTH || !VALID_FORMAT.matcher(normalizedValue).matches()) {
            throw new InvalidUserEmailException(rawValue);
        }
        return normalizedValue;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserEmail userEmail)) {
            return false;
        }
        return value.equals(userEmail.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
