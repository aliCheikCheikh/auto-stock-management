package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.domain.model.user.UserId;

public interface UserSessionRevoker {

    void revokeAll(UserId userId);
}
