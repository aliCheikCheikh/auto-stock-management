package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.user.LastActiveOwnerException;
import com.aliCheikh.stock.domain.model.user.User;

import java.util.Objects;

/** Ensures an active owner remains able to administer the shop. */
public final class OwnerContinuity {

    public void ensureDeactivationKeepsAnActiveOwner(User userToDeactivate, long activeOwnerCount) {
        Objects.requireNonNull(userToDeactivate, "User to deactivate is required.");
        if (activeOwnerCount < 0) {
            throw new IllegalArgumentException("Active owner count must not be negative.");
        }
        if (userToDeactivate.isOwner() && userToDeactivate.isActive() && activeOwnerCount <= 1) {
            throw new LastActiveOwnerException();
        }
    }
}
