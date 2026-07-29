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

/**
 * Résout en un seul appel les noms affichables d'un lot d'utilisateurs.
 *
 * <p>Un historique désigne ses auteurs par identifiant technique ; l'utilisateur, lui, attend un
 * nom — « c'est Ahmat qui a fait cette réception ». Résoudre chaque ligne séparément produirait un
 * N+1, d'où cette résolution par page. Le service est partagé entre les historiques de mouvements
 * et de ventes plutôt que dupliqué dans chacun.</p>
 */
@Component
public class UserDisplayNameResolver {

    private final UserJpaRepository userJpaRepository;

    public UserDisplayNameResolver(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository, "userJpaRepository cannot be null");
    }

    /**
     * @param userIds identifiants, doublons et {@code null} tolérés
     * @return les noms trouvés, indexés par identifiant ; un compte supprimé est simplement absent
     */
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
