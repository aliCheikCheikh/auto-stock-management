package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthFlowIntegrationTest {

    private static final String OWNER_EMAIL = "owner@test.local";
    private static final String OWNER_PASSWORD = "Secret123!";
    private static final String NEW_PASSWORD = "NewSecret456!";

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auto_stock_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        UserJpaEntity owner = UserJpaEntity.withCredentials(
                UUID.randomUUID(),
                OWNER_EMAIL,
                OWNER_EMAIL,
                passwordEncoder.encode(OWNER_PASSWORD),
                UserRole.OWNER);
        userRepository.save(owner);
    }

    @Test
    void login_succeeds_and_me_returns_identity() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = login.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").cookie(accessCookie))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_is_rejected_after_logout() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = login.getResponse().getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();

        // le refresh marche AVANT le logout
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk());

        // le logout révoque le refresh token
        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent());

        // le refresh est maintenant refusé
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void change_password_succeeds_then_new_password_works_and_old_is_rejected() throws Exception {
        Cookie accessCookie = loginAndGetAccessCookie(OWNER_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(OWNER_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // L'ancien mot de passe ne fonctionne plus.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(OWNER_EMAIL, OWNER_PASSWORD)))
                .andExpect(status().isUnauthorized());

        // Le nouveau mot de passe fonctionne.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(OWNER_EMAIL, NEW_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void change_password_clears_temporary_flag() throws Exception {
        assertThat(userRepository.findByEmail(OWNER_EMAIL).orElseThrow().isPasswordTemporary()).isTrue();

        Cookie accessCookie = loginAndGetAccessCookie(OWNER_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(OWNER_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(OWNER_EMAIL).orElseThrow().isPasswordTemporary()).isFalse();
    }

    @Test
    void change_password_with_wrong_current_password_is_unauthorized() throws Exception {
        Cookie accessCookie = loginAndGetAccessCookie(OWNER_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"WrongPassword!","newPassword":"%s"}
                                """.formatted(NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void change_password_with_too_short_new_password_is_bad_request() throws Exception {
        Cookie accessCookie = loginAndGetAccessCookie(OWNER_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"short"}
                                """.formatted(OWNER_PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void change_password_requires_authentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(OWNER_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private Cookie loginAndGetAccessCookie(String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(OWNER_EMAIL, password)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = login.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();
        return accessCookie;
    }
}