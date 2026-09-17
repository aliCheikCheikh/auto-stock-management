package com.aliCheikh.stock.infrastructure.web.filter;

import com.aliCheikh.stock.infrastructure.persistence.entity.IdempotencyRecordJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private final IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    public IdempotencyFilter(IdempotencyRecordJpaRepository idempotencyRecordJpaRepository) {
        this.idempotencyRecordJpaRepository = idempotencyRecordJpaRepository;
    }

    private final Set<String> IDEMPOTENT_PATHS = Set.of(
            "/api/v1/sales",
            "/api/v1/stock-receipts",
            "/api/v1/stock-transfers"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equals(request.getMethod()) || !IDEMPOTENT_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID key;
        try {
            key = UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyRecordJpaEntity> existing = idempotencyRecordJpaRepository.findById(key);
        if (existing.isPresent()) {
            IdempotencyRecordJpaEntity record = existing.get();

            byte[] requestBody = request.getInputStream().readAllBytes();
            String incomingHash = sha256(requestBody);

            if (incomingHash.equals(record.getRequestHash())) {
                response.setStatus(record.getResponseStatus());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(record.getResponseBody());
                return;
            }

            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"IDEMPOTENCY_KEY_REUSED\"}");
            return;

        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(wrappedRequest, wrappedResponse);

        int status = wrappedResponse.getStatus();

        if (status < 500) {
            byte[] requestBody = wrappedRequest.getContentAsByteArray();
            String requestHash = sha256(requestBody);
            String responseBody = new String(
                    wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

            IdempotencyRecordJpaEntity record = IdempotencyRecordJpaEntity.of(
                    key,
                    request.getMethod(),
                    request.getRequestURI(),
                    requestHash,
                    status,
                    responseBody,
                    Instant.now()
            );
            idempotencyRecordJpaRepository.save(record);
        }

        wrappedResponse.copyBodyToResponse();


    }

    private String sha256(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(body);

            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
