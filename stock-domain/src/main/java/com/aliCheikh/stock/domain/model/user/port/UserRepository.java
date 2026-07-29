package com.aliCheikh.stock.domain.model.user.port;

import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    List<User> findAll();

    Optional<User> findById(UserId userId);

    Optional<User> findByEmail(UserEmail email);

    boolean existsByEmail(UserEmail email);

    List<User> findActiveOwners();

    void save(User user);
}
