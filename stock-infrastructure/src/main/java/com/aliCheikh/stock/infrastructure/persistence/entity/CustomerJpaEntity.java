package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class CustomerJpaEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "given_name", nullable = false, length = 100)
    private String givenName;
    @Column(name = "father_name", length = 100)
    private String fatherName;
    @Column(name = "phone_number", nullable = false, length = 100)
    private String phoneNumber;
    @Column(name = "email", length = 100)
    private String email;

    protected CustomerJpaEntity() {

    }

    private CustomerJpaEntity(UUID id,
                              String givenName,
                              String fatherName,
                              String phoneNumber,
                              String email) {
        this.id = Objects.requireNonNull(id, "id is null");
        this.givenName = Objects.requireNonNull(givenName, "givenName is null");
        this.fatherName = fatherName;
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber is null");
        this.email = email;
    }

    public static CustomerJpaEntity of(UUID id, String givenName, String fatherName, String phoneNumber, String email) {
        return new CustomerJpaEntity(id, givenName, fatherName, phoneNumber, email);
    }

    public UUID getId() {
        return id;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFatherName() {
        return fatherName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }
}
