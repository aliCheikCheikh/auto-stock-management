package com.aliCheikh.stock.infrastructure.security;

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

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = readTokenFromCookie(request);
        if (token != null) {
            try {
                AuthenticatedUser user = jwtService.parse(token);

                var authority = new SimpleGrantedAuthority("ROLE_" + user.role());
                var authentication = new UsernamePasswordAuthenticationToken(user.userId(),
                        null,
                        List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);


            } catch (JwtException e) {
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
