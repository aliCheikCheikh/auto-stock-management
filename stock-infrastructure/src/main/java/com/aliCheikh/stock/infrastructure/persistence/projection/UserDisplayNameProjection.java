package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.util.UUID;

/** Identifiant et nom affichable d'un utilisateur, sans charger le reste du compte. */
public interface UserDisplayNameProjection {

    UUID getId();

    String getDisplayName();
}
