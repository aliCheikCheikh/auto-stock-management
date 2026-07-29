package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.application.usecase.GetUserUseCase;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String TOKEN_COOKIE_NAME = "access_token";
    private final JwtService jwtService;
    private final GetUserUseCase getUserUseCase;

    public JwtAuthenticationFilter(JwtService jwtService, GetUserUseCase getUserUseCase) {
        this.jwtService = jwtService;
        this.getUserUseCase = getUserUseCase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = readTokenFromCookie(request);
        if (token != null) {
            try {
                AuthenticatedUser user = jwtService.parse(token);
                User currentUser = getUserUseCase.execute(UserId.of(user.userId()));
                if (!currentUser.isActive()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                var authority = new SimpleGrantedAuthority("ROLE_" + currentUser.getRole().name());
                var authentication = new UsernamePasswordAuthenticationToken(user.userId(),
                        null,
                        List.of(authority));
                authentication.setDetails(new UserAccessState(currentUser.isPasswordChangeRequired()));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | UserNotFoundException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);

    }

    private String readTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
