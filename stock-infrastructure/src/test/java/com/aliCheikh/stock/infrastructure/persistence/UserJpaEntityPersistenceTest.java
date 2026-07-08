package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserJpaEntityPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void should_persist_new_credentialed_user_as_password_temporary() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserJpaEntity newOwner = UserJpaEntity.withCredentials(userId,
                "User Test",
                "usertest@autostock.com",
                "hash_quelconque",
                UserRole.OWNER);
        userJpaRepository.save(newOwner);
        entityManager.flush();
        entityManager.clear();

        Optional<UserJpaEntity> persistedUser = userJpaRepository.findById(userId);

        assertThat(persistedUser).isPresent();
        UserJpaEntity persisted = persistedUser.get();
        assertThat(persisted.getId()).isEqualTo(userId);
        assertThat(persisted.isPasswordTemporary()).isTrue();
    }

}
