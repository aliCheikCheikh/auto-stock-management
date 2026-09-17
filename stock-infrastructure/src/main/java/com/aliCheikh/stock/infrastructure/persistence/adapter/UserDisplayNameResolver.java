package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.infrastructure.persistence.projection.UserDisplayNameProjection;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Resolves user display names for a batch of IDs, shared by sale and movement history. */
@Component
public class UserDisplayNameResolver {

    private final UserJpaRepository userJpaRepository;

    public UserDisplayNameResolver(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository, "userJpaRepository cannot be null");
    }

    /** Accepts duplicate and null IDs. Missing accounts are omitted from the result. */
    @Transactional(readOnly = true)
    public Map<UUID, String> resolve(Collection<UUID> userIds) {
        Objects.requireNonNull(userIds, "userIds cannot be null");

        Set<UUID> distinctIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (distinctIds.isEmpty()) {
            return Map.of();
        }

        return userJpaRepository.findDisplayNames(distinctIds).stream()
                .collect(Collectors.toMap(
                        UserDisplayNameProjection::getId,
                        UserDisplayNameProjection::getDisplayName));
    }
}
