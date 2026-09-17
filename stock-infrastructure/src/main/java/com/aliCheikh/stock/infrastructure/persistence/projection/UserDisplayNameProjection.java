package com.aliCheikh.stock.infrastructure.persistence.projection;

import java.util.UUID;

/** User identity and display name without loading the full account. */
public interface UserDisplayNameProjection {

    UUID getId();

    String getDisplayName();
}
