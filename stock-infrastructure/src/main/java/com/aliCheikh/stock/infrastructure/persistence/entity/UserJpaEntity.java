package com.aliCheikh.stock.infrastructure.persistence.entity;

import com.aliCheikh.stock.domain.model.user.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "password_temporary", nullable = false)
    private boolean passwordTemporary;

    // Initialisé à true : tout nouveau compte naît actif. L'initialiseur s'exécute
    // dans chaque constructeur (donc via withCredentials aussi), et Hibernate écrase
    // avec la valeur de la base au chargement.
    @Column(name = "active", nullable = false)
    private boolean active = true;


    protected UserJpaEntity() {
    }

    private UserJpaEntity(UUID id, String username, UserRole role) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
    }

    private UserJpaEntity(UUID id, String username, String email, String passwordHash, UserRole role) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.email = Objects.requireNonNull(email, "email cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.passwordTemporary = true;
    }

    public static UserJpaEntity of(UUID id, String username, UserRole role) {
        return new UserJpaEntity(id, username, role);
    }

    public static UserJpaEntity withCredentials(UUID id, String username, String email, String passwordHash, UserRole role) {
        return new UserJpaEntity(id, username, email, passwordHash, role);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "passwordHash cannot be null");
        this.passwordTemporary = false;
    }

    public void deactivate() {
        this.active = false;
    }

    // Repose un mot de passe temporaire : l'utilisateur devra le changer au prochain login.
    public void resetPassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "passwordHash cannot be null");
        this.passwordTemporary = true;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isPasswordTemporary() {
        return passwordTemporary;
    }

    public boolean isActive() {
        return active;
    }
}
