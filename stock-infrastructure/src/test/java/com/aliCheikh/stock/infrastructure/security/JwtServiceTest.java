package com.aliCheikh.stock.infrastructure.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtServiceTest {

    @Test
    void generates_a_signed_access_token() {
        JwtService jwtService = new JwtService("test-secret-0123456789-abcdefghij-KLMNOP");
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "OWNER");
        System.out.println("TOKEN = " + token);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void parses_back_a_token_it_generated() {
        JwtService jwtService = new JwtService("test-secret-0123456789-abcdefghij-KLMNOP");
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, "OWNER");
        AuthenticatedUser user = jwtService.parse(token);

        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.role()).isEqualTo("OWNER");
    }

    @Test
    void rejects_a_tampered_token() {
        JwtService jwtService = new JwtService("test-secret-0123456789-abcdefghij-KLMNOP");
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "OWNER");

        String tampered = token + "x";

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtException.class);
    }
}
