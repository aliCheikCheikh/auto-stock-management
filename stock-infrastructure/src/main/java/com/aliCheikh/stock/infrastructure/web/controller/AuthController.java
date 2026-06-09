package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.infrastructure.persistence.entity.UserJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.UserJpaRepository;
import com.aliCheikh.stock.infrastructure.security.JwtService;
import com.aliCheikh.stock.infrastructure.web.dto.LoginRequest;
import com.aliCheikh.stock.infrastructure.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userJpaRepository;
    private final JwtService jwtService;
    private final Boolean cookieSecure;

    public AuthController(PasswordEncoder passwordEncoder,
                          UserJpaRepository userJpaRepository,
                          JwtService jwtService,
                          @Value("${app.security.cookie.secure:false}") Boolean cookieSecure) {
        this.passwordEncoder = passwordEncoder;
        this.userJpaRepository = userJpaRepository;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String email = loginRequest.email().trim().toLowerCase();
        UserJpaEntity user = userJpaRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        ResponseCookie cookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("strict").path("/")
                .maxAge(Duration.ofMinutes(15))
                .build();

        LoginResponse loginResponse = new LoginResponse(user.getId(), user.getRole().name());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponse);

    }

}
