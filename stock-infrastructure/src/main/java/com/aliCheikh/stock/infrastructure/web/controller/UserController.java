package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import com.aliCheikh.stock.infrastructure.web.dto.CreateUserRequest;
import com.aliCheikh.stock.infrastructure.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserJpaRepository userJpaRepository, PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createSeller(@Valid @RequestBody CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userJpaRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Le rôle est fixé côté serveur : cet endpoint ne crée que des SELLER.
        // Le compte naît avec un mot de passe temporaire (via withCredentials).
        UserJpaEntity seller = UserJpaEntity.withCredentials(
                UUID.randomUUID(),
                email,
                email,
                passwordEncoder.encode(request.temporaryPassword()),
                UserRole.SELLER);
        userJpaRepository.save(seller);

        UserResponse body = new UserResponse(seller.getId(), seller.getEmail(), seller.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
