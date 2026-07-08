package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.adapter.ProductJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.entity.CategoryJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
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

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductOwnerEndpointsIntegrationTest {

    private static final String OWNER_EMAIL = "owner@test.local";
    private static final String OWNER_PASSWORD = "Secret123!";
    private static final String SELLER_EMAIL = "seller@test.local";
    private static final String SELLER_PASSWORD = "Secret456!";

    private static final String UPDATE_REQUEST_BODY = """
            {
              "name": "Disque de frein",
              "unitPrice": { "amount": "59.90", "currency": "EUR" },
              "minimumGlobalThreshold": 8
            }
            """;

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
    private CategoryJpaRepository categoryRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductJpaRepositoryAdapter productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ProductId productId;
    private CategoryId categoryId;

    @BeforeEach
    void setUp() {
        // Nettoyage dans l'ordre des clés étrangères : enfant avant parent.
        productJpaRepository.deleteAll();    // product -> category
        categoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();  // refresh_token -> user
        userRepository.deleteAll();

        saveEstablishedUser(OWNER_EMAIL, OWNER_PASSWORD, UserRole.OWNER);
        saveEstablishedUser(SELLER_EMAIL, SELLER_PASSWORD, UserRole.SELLER);

        productId = ProductId.generate();
        categoryId = CategoryId.generate();

        categoryRepository.save(CategoryJpaEntity.of(categoryId.getValue(), "Brakes"));
        productRepository.save(new Product(
                productId, "Brake pads", "BRK-PAD-001", categoryId, 5,
                Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"))));
    }

    // Les fixtures représentent des comptes déjà onboardés : on efface l'indicateur
    // "mot de passe temporaire" pour qu'ils ne soient pas bloqués par le TemporaryPasswordFilter.
    private void saveEstablishedUser(String email, String password, UserRole role) {
        UserJpaEntity user = UserJpaEntity.withCredentials(
                UUID.randomUUID(), email, email, passwordEncoder.encode(password), role);
        user.changePassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Test
    void should_return_200_when_update_product_with_owner_role() throws Exception {
        Cookie ownerCookie = loginAndGetCookie(OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(put("/api/v1/products/{productId}", productId.getValue())
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/{productId}", productId.getValue())
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Disque de frein"))
                .andExpect(jsonPath("$.unitPrice.amount").value("59.90"))
                .andExpect(jsonPath("$.minimumGlobalThreshold").value(8));
    }

    @Test
    void should_return_403_when_seller_tries_to_update_product() throws Exception {
        Cookie sellerCookie = loginAndGetCookie(SELLER_EMAIL, SELLER_PASSWORD);

        mockMvc.perform(put("/api/v1/products/{productId}", productId.getValue())
                        .cookie(sellerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return_403_when_seller_tries_to_deactivate_product() throws Exception {
        Cookie sellerCookie = loginAndGetCookie(SELLER_EMAIL, SELLER_PASSWORD);

        mockMvc.perform(delete("/api/v1/products/{productId}", productId.getValue())
                        .cookie(sellerCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return_401_when_updating_product_without_authentication() throws Exception {
        mockMvc.perform(put("/api/v1/products/{productId}", productId.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_soft_delete_product_when_owner_deactivates_it() throws Exception {
        Cookie ownerCookie = loginAndGetCookie(OWNER_EMAIL, OWNER_PASSWORD);

        mockMvc.perform(delete("/api/v1/products/{productId}", productId.getValue())
                        .cookie(ownerCookie))
                .andExpect(status().isNoContent());

        // Soft delete : la ligne existe toujours en base, mais inactive (historique préservé).
        Optional<Product> persisted = productRepository.findById(productId);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().isActive()).isFalse();

        // Et il disparaît du catalogue actif (sélecteurs).
        mockMvc.perform(get("/api/v1/products").param("activeOnly", "true")
                        .cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void should_return_404_when_owner_updates_unknown_product() throws Exception {
        Cookie ownerCookie = loginAndGetCookie(OWNER_EMAIL, OWNER_PASSWORD);
        UUID unknownProductId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/products/{productId}", unknownProductId)
                        .cookie(ownerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isNotFound());
    }

    private Cookie loginAndGetCookie(String email, String password) throws Exception {
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
