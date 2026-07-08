package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Tant que le mot de passe d'un utilisateur est temporaire, il ne peut rien faire
 * d'autre que passer par les routes d'authentification (dont le changement de mot
 * de passe). Toute autre requête est refusée avec un 403.
 */
public class TemporaryPasswordFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final UserJpaRepository userJpaRepository;

    public TemporaryPasswordFilter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Liste blanche : les routes d'auth restent ouvertes, sinon un utilisateur
        // temporaire ne pourrait jamais atteindre change-password pour se débloquer.
        if (request.getRequestURI().startsWith(AUTH_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            boolean passwordTemporary = userJpaRepository.findById(userId)
                    .map(UserJpaEntity::isPasswordTemporary)
                    .orElse(false);
            if (passwordTemporary) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
