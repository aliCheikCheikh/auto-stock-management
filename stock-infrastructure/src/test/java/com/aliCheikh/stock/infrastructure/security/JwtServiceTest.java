package com.aliCheikh.stock.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtServiceTest {

    @Test
    void generates_a_signed_access_token() {
        JwtService jwtService = new JwtService("test-secret-0123456789-abcdefghij-KLMNOP");
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "OWNER");
        System.out.println("TOKEN = " + token);
        assertThat(token.split("\\.")).hasSize(3);
    }
}
