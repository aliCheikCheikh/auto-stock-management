package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InitialOwnerSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InitialOwnerSeeder.class);

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String ownerEmail;
    private final String ownerPassword;

    public InitialOwnerSeeder(UserJpaRepository userJpaRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${INITIAL_OWNER_EMAIL:}") String ownerEmail,
                              @Value("${INITIAL_OWNER_PASSWORD:}") String ownerPassword) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.ownerEmail = ownerEmail;
        this.ownerPassword = ownerPassword;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (ownerEmail.isBlank() || ownerPassword.isBlank()) {
            log.info("Seeding owner ignoré : INITIAL_OWNER_PASSWORD/EMAIL non définis");
            return;
        }

        String email = ownerEmail.trim().toLowerCase();

        if (userJpaRepository.findByEmail(email).isPresent()) {
            log.info("Owner déjà présent, seeding ignoré");
            return;
        }

        UserJpaEntity owner = UserJpaEntity.withCredentials(UUID.randomUUID(),
                email,
                email,
                passwordEncoder.encode(ownerPassword),
                UserRole.OWNER);
        userJpaRepository.save(owner);
        log.info("Owner initial créé ppur l'email {}.", email);


    }
}
