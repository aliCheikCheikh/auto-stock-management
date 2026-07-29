package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.user.LastActiveOwnerException;
import com.aliCheikh.stock.domain.model.user.User;

import java.util.Objects;

/** Préserve la présence d'un propriétaire capable d'administrer le magasin. */
public final class OwnerContinuity {

    public void ensureDeactivationKeepsAnActiveOwner(User userToDeactivate, long activeOwnerCount) {
        Objects.requireNonNull(userToDeactivate, "L'utilisateur à désactiver est obligatoire.");
        if (activeOwnerCount < 0) {
            throw new IllegalArgumentException("Le nombre de propriétaires actifs ne peut pas être négatif.");
        }
        if (userToDeactivate.isOwner() && userToDeactivate.isActive() && activeOwnerCount <= 1) {
            throw new LastActiveOwnerException();
        }
    }
}
