package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.application.port.PasswordProtection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class BCryptPasswordProtection implements PasswordProtection {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordProtection(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder cannot be null");
    }

    @Override
    public String protect(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean matches(String password, String protectedPassword) {
        return passwordEncoder.matches(password, protectedPassword);
    }
}
