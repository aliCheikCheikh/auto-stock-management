package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.Optional;

public interface UserCredentialStore {

    Optional<String> findProtectedPassword(UserId userId);

    void replaceProtectedPassword(UserId userId, String protectedPassword);
}
