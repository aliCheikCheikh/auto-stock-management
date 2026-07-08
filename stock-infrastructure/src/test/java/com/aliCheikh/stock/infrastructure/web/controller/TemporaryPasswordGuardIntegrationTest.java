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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TemporaryPasswordGuardIntegrationTest {

    private static final String OWNER_EMAIL = "temp-owner@test.local";
    private static final String TEMP_PASSWORD = "TempPass123!";
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
        // Compte fraîchement provisionné : mot de passe temporaire (flag = true par défaut).
        userRepository.save(UserJpaEntity.withCredentials(
                UUID.randomUUID(), OWNER_EMAIL, OWNER_EMAIL,
                passwordEncoder.encode(TEMP_PASSWORD), UserRole.OWNER));
    }

    @Test
    void temporary_user_is_blocked_on_business_endpoint() throws Exception {
        Cookie accessCookie = login(TEMP_PASSWORD);

        mockMvc.perform(get("/api/v1/categories").cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void temporary_user_can_still_change_password() throws Exception {
        Cookie accessCookie = login(TEMP_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(TEMP_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    void user_is_unblocked_after_changing_password() throws Exception {
        Cookie accessCookie = login(TEMP_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(TEMP_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // Le même cookie passe désormais : le filtre relit le flag en base (maintenant false).
        mockMvc.perform(get("/api/v1/categories").cookie(accessCookie))
                .andExpect(status().isOk());
    }

    @Test
    void login_and_me_expose_the_temporary_flag() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(OWNER_EMAIL, TEMP_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordTemporary").value(true))
                .andReturn();

        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(TEMP_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordTemporary").value(false));
    }

    private Cookie login(String password) throws Exception {
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
