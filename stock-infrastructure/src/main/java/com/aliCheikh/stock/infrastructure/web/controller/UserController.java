package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.domain.model.user.UserRole;
import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import com.aliCheikh.stock.infrastructure.web.dto.CreateUserRequest;
import com.aliCheikh.stock.infrastructure.web.dto.ResetPasswordRequest;
import com.aliCheikh.stock.infrastructure.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(seller));
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userJpaRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
        UserJpaEntity user = userJpaRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // On ne désactive que des vendeurs : jamais l'OWNER (sinon on verrouille tout).
        if (user.getRole() != UserRole.SELLER) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        user.deactivate();
        userJpaRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID userId,
                                              @Valid @RequestBody ResetPasswordRequest request) {
        UserJpaEntity user = userJpaRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // On ne réinitialise que des vendeurs (l'OWNER passe par change-password lui-même).
        if (user.getRole() != UserRole.SELLER) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        user.resetPassword(passwordEncoder.encode(request.temporaryPassword()));
        userJpaRepository.save(user);
        return ResponseEntity.noContent().build();
    }
}
