package com.aliCheikh.stock.domain.model.user;

import com.aliCheikh.stock.domain.exception.user.InvalidUserNameException;

import java.util.Objects;

public class User {
    private final UserId userId;
    private String userName;
    private UserRole userRole;

    public User(UserId userId, String userName, UserRole userRole) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.userName = validateUserName(userName);
        this.userRole = validateUserRole(userRole);
    }

    public void rename(String newName) {
        this.userName = validateUserName(newName);
    }

    public void changeUserRole(UserRole newUserRole) {
        this.userRole = validateUserRole(newUserRole);
    }


    private String validateUserName(String nameToValidate) {
        if (nameToValidate == null || nameToValidate.isBlank()) {
            throw new InvalidUserNameException(nameToValidate);
        }
        return nameToValidate;
    }

    private UserRole validateUserRole(UserRole roleToValidate) {
        return Objects.requireNonNull(roleToValidate, "userRole cannot be null");
    }

    // --- Getters ---
    public UserId getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;
        return userId.equals(user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}