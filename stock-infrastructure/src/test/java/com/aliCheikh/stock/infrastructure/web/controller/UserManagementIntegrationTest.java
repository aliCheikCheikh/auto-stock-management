package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.domain.model.user.UserRole;
import com.jayway.jsonpath.JsonPath;
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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserManagementIntegrationTest {

    private static final String OWNER_EMAIL = "owner@test.local";
    private static final String OWNER_PASSWORD = "OwnerPass123!";
    private static final String SELLER_EMAIL = "seller@test.local";
    private static final String SELLER_PASSWORD = "SellerPass123!";
    private static final String NEW_SELLER_EMAIL = "new-seller@test.local";
    private static final String NEW_SELLER_NAME = "Amina Mahamat";

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
        saveEstablishedUser(OWNER_EMAIL, OWNER_PASSWORD, UserRole.OWNER);
        saveEstablishedUser(SELLER_EMAIL, SELLER_PASSWORD, UserRole.SELLER);
    }

    @Test
    void owner_creates_a_seller_who_can_login_but_must_change_password() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);

        MvcResult creation = mockMvc.perform(post("/api/v1/users")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","email":"%s"}
                                """.formatted(NEW_SELLER_NAME, NEW_SELLER_EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(NEW_SELLER_EMAIL))
                .andExpect(jsonPath("$.user.displayName").value(NEW_SELLER_NAME))
                .andExpect(jsonPath("$.user.role").value("SELLER"))
                .andReturn();

        String temporaryPassword = JsonPath.read(
                creation.getResponse().getContentAsString(), "$.temporaryPassword");

        // Le nouveau vendeur peut se connecter, mais son mot de passe est temporaire.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(NEW_SELLER_EMAIL, temporaryPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordTemporary").value(true));
    }

    @Test
    void a_seller_cannot_create_a_user() throws Exception {
        Cookie sellerCookie = login(SELLER_EMAIL, SELLER_PASSWORD);

        mockMvc.perform(post("/api/v1/users")
                        .cookie(sellerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","email":"%s"}
                                """.formatted(NEW_SELLER_NAME, NEW_SELLER_EMAIL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void creating_a_user_with_an_existing_email_is_conflict() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(post("/api/v1/users")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","email":"%s"}
                                """.formatted(NEW_SELLER_NAME, SELLER_EMAIL)))
                .andExpect(status().isConflict());
    }

    @Test
    void creating_a_user_without_a_display_name_is_bad_request() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(post("/api/v1/users")
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":" ","email":"%s"}
                                """.formatted(NEW_SELLER_EMAIL)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creating_a_user_without_authentication_is_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"displayName":"%s","email":"%s"}
                                """.formatted(NEW_SELLER_NAME, NEW_SELLER_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void owner_lists_all_accounts_without_leaking_password_hash() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(get("/api/v1/users").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", hasItems(OWNER_EMAIL, SELLER_EMAIL)))
                .andExpect(jsonPath("$[0].displayName").exists())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void a_seller_cannot_list_accounts() throws Exception {
        Cookie sellerCookie = login(SELLER_EMAIL, SELLER_PASSWORD);

        mockMvc.perform(get("/api/v1/users").cookie(sellerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void listing_accounts_without_authentication_is_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void business_data_is_private_by_default() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void health_check_remains_public() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    void owner_deactivates_a_seller_who_can_no_longer_login() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);
        UUID sellerId = userRepository.findByEmail(SELLER_EMAIL).orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/users/{id}", sellerId).cookie(ownerCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(SELLER_EMAIL, SELLER_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivation_invalidates_an_access_cookie_immediately() throws Exception {
        Cookie sellerCookie = login(SELLER_EMAIL, SELLER_PASSWORD);
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);
        UUID sellerId = userRepository.findByEmail(SELLER_EMAIL).orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/users/{id}", sellerId).cookie(ownerCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categories").cookie(sellerCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void owner_can_rename_and_reactivate_a_seller() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);
        UUID sellerId = userRepository.findByEmail(SELLER_EMAIL).orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/users/{id}/display-name", sellerId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Moussa Saleh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Moussa Saleh"));

        mockMvc.perform(delete("/api/v1/users/{id}", sellerId).cookie(ownerCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/users/{id}/reactivate", sellerId).cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.displayName").value("Moussa Saleh"));
    }

    @Test
    void deactivating_the_owner_is_rejected() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);
        UUID ownerId = userRepository.findByEmail(OWNER_EMAIL).orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/users/{id}", ownerId).cookie(ownerCookie))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivating_an_unknown_user_is_not_found() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(delete("/api/v1/users/{id}", UUID.randomUUID()).cookie(ownerCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void a_seller_cannot_deactivate_a_user() throws Exception {
        Cookie sellerCookie = login(SELLER_EMAIL, SELLER_PASSWORD);
        UUID sellerId = userRepository.findByEmail(SELLER_EMAIL).orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/users/{id}", sellerId).cookie(sellerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void owner_resets_a_seller_password_forcing_a_new_forced_change() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);
        UUID sellerId = userRepository.findByEmail(SELLER_EMAIL).orElseThrow().getId();

        MvcResult reset = mockMvc.perform(post("/api/v1/users/{id}/reset-password", sellerId)
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andReturn();
        String resetTemporaryPassword = JsonPath.read(
                reset.getResponse().getContentAsString(), "$.temporaryPassword");

        // L'ancien mot de passe ne marche plus.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(SELLER_EMAIL, SELLER_PASSWORD)))
                .andExpect(status().isUnauthorized());

        // Le nouveau mot de passe temporaire marche, et force un changement.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(SELLER_EMAIL, resetTemporaryPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordTemporary").value(true));
    }

    @Test
    void resetting_the_owner_password_is_rejected() throws Exception {
        Cookie ownerCookie = login(OWNER_EMAIL, OWNER_PASSWORD);
        UUID ownerId = userRepository.findByEmail(OWNER_EMAIL).orElseThrow().getId();

        mockMvc.perform(post("/api/v1/users/{id}/reset-password", ownerId)
                        .cookie(ownerCookie))
                .andExpect(status().isConflict());
    }

    @Test
    void a_seller_cannot_reset_a_password() throws Exception {
        Cookie sellerCookie = login(SELLER_EMAIL, SELLER_PASSWORD);
        UUID sellerId = userRepository.findByEmail(SELLER_EMAIL).orElseThrow().getId();

        mockMvc.perform(post("/api/v1/users/{id}/reset-password", sellerId)
                        .cookie(sellerCookie))
                .andExpect(status().isForbidden());
    }

    private void saveEstablishedUser(String email, String password, UserRole role) {
        UserJpaEntity user = UserJpaEntity.withCredentials(
                UUID.randomUUID(), email, email, passwordEncoder.encode(password), role);
        user.changePassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    private Cookie login(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessCookie = login.getResponse().getCookie("access_token");
        assertThat(accessCookie).isNotNull();
        return accessCookie;
    }
}
