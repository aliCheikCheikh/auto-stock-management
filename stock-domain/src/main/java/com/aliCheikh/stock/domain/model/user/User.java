package com.aliCheikh.stock.domain.model.user;

import com.aliCheikh.stock.domain.exception.user.InvalidUserNameException;

import java.util.Objects;

/** User authorized to work in the shop. */
public final class User {

    public static final int MAX_DISPLAY_NAME_LENGTH = 100;

    private final UserId id;
    private String displayName;
    private final UserEmail email;
    private final UserRole role;
    private boolean active;
    private boolean passwordChangeRequired;

    public User(UserId id,
                String displayName,
                UserEmail email,
                UserRole role,
                boolean active,
                boolean passwordChangeRequired) {
        this.id = Objects.requireNonNull(id, "User ID is required.");
        this.displayName = validateDisplayName(displayName);
        this.email = Objects.requireNonNull(email, "User email is required.");
        this.role = Objects.requireNonNull(role, "User role is required.");
        this.active = active;
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public static User newSeller(String displayName, UserEmail email) {
        return new User(UserId.generate(), displayName, email, UserRole.SELLER, true, true);
    }

    public static User newOwner(String displayName, UserEmail email) {
        return new User(UserId.generate(), displayName, email, UserRole.OWNER, true, true);
    }

    public void rename(String newDisplayName) {
        displayName = validateDisplayName(newDisplayName);
    }

    public void deactivate() {
        active = false;
    }

    public void reactivate() {
        active = true;
    }

    public void requirePasswordChange() {
        passwordChangeRequired = true;
    }

    public void confirmPasswordChange() {
        passwordChangeRequired = false;
    }

    public boolean isOwner() {
        return role == UserRole.OWNER;
    }

    private String validateDisplayName(String value) {
        if (value == null || value.isBlank() || value.trim().length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new InvalidUserNameException(value, MAX_DISPLAY_NAME_LENGTH);
        }
        return value.trim();
    }

    public UserId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserEmail getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
