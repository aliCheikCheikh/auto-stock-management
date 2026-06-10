package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import com.aliCheikh.stock.infrastructure.security.JwtService;
import com.aliCheikh.stock.infrastructure.security.RefreshTokenService;
import com.aliCheikh.stock.infrastructure.web.dto.LoginRequest;
import com.aliCheikh.stock.infrastructure.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userJpaRepository;
    private final JwtService jwtService;
    private final Boolean cookieSecure;
    private final RefreshTokenService refreshTokenService;

    public AuthController(PasswordEncoder passwordEncoder,
                          UserJpaRepository userJpaRepository,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          @Value("${app.security.cookie.secure:false}") Boolean cookieSecure) {
        this.passwordEncoder = passwordEncoder;
        this.userJpaRepository = userJpaRepository;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return ResponseEntity.ok(new LoginResponse(userId, role));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String email = loginRequest.email().trim().toLowerCase();
        UserJpaEntity user = userJpaRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user.getId());
        ResponseCookie cookie = buildAccessCookie(accessToken);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        LoginResponse body = new LoginResponse(user.getId(), user.getRole().name());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);

    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserJpaEntity user = refreshTokenService.validate(refreshToken)
                .flatMap(userJpaRepository::findById)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        ResponseCookie accessCookie = buildAccessCookie(accessToken);

        LoginResponse body = new LoginResponse(user.getId(), user.getRole().name());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .build();


    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }

        ResponseCookie clearedAccess = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie clearedRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedAccess.toString())
                .header(HttpHeaders.SET_COOKIE, clearedRefresh.toString())
                .build();
    }

    private ResponseCookie buildAccessCookie(String accessToken) {
        return ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .build();
    }


}
