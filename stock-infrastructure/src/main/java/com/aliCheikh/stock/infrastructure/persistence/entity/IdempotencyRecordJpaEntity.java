package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecordJpaEntity {
    @Id
    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "request_method", length = 10, nullable = false)
    private String requestMethod;

    @Column(name = "request_path", length = 255, nullable = false)
    private String requestPath;

    @Column(name = "request_hash", length = 64, nullable = false)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecordJpaEntity() {
    }

    private IdempotencyRecordJpaEntity(UUID idempotencyKey,
                                       String requestMethod,
                                       String requestPath,
                                       String requestHash,
                                       int responseStatus,
                                       String responseBody,
                                       Instant createdAt) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey cannot be null");
        this.requestMethod = Objects.requireNonNull(requestMethod, "requestMethod cannot be null");
        this.requestPath = Objects.requireNonNull(requestPath, "requestPath cannot be null");
        this.requestHash = Objects.requireNonNull(requestHash, "requestHash cannot be null");
        this.responseStatus = responseStatus;
        this.responseBody = Objects.requireNonNull(responseBody, "responseBody cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");

    }

    public static IdempotencyRecordJpaEntity of(UUID idempotencyKey, String requestMethod,
                                                String requestPath,
                                                String requestHash,
                                                int responseStatus,
                                                String responseBody,
                                                Instant createdAt) {
        return new IdempotencyRecordJpaEntity(idempotencyKey,
                requestMethod,
                requestPath,
                requestHash,
                responseStatus,
                responseBody,
                createdAt);
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
