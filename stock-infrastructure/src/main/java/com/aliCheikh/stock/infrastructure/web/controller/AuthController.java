package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ChangeOwnPasswordCommand;
import com.aliCheikh.stock.application.usecase.AuthenticateUserUseCase;
import com.aliCheikh.stock.application.usecase.ChangeOwnPasswordUseCase;
import com.aliCheikh.stock.application.usecase.GetUserUseCase;
import com.aliCheikh.stock.domain.exception.user.UserNotFoundException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.security.JwtService;
import com.aliCheikh.stock.infrastructure.security.RefreshTokenService;
import com.aliCheikh.stock.infrastructure.web.dto.ChangePasswordRequest;
import com.aliCheikh.stock.infrastructure.web.dto.LoginRequest;
import com.aliCheikh.stock.infrastructure.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ChangeOwnPasswordUseCase changeOwnPasswordUseCase;
    private final JwtService jwtService;
    private final Boolean cookieSecure;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase,
                          GetUserUseCase getUserUseCase,
                          ChangeOwnPasswordUseCase changeOwnPasswordUseCase,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          @Value("${app.security.cookie.secure:false}") Boolean cookieSecure) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.changeOwnPasswordUseCase = changeOwnPasswordUseCase;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(Authentication authentication) {
        User user = findUser((UUID) authentication.getPrincipal());
        return user == null
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                : ResponseEntity.ok(toLoginResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        User user = authenticateUserUseCase
                .execute(UserEmail.of(loginRequest.email()), loginRequest.password())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user.getId().getValue(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user.getId().getValue());
        ResponseCookie accessCookie = buildAccessCookie(accessToken);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(toLoginResponse(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = refreshTokenService.validate(refreshToken).map(this::findUser).orElse(null);
        if (user == null || !user.isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtService.generateAccessToken(user.getId().getValue(), user.getRole().name());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAccessCookie(accessToken).toString())
                .body(toLoginResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
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

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        changeOwnPasswordUseCase.execute(new ChangeOwnPasswordCommand(
                UserId.of(userId),
                request.currentPassword(),
                request.newPassword()));
        return ResponseEntity.noContent().build();
    }

    private User findUser(UUID userId) {
        try {
            return getUserUseCase.execute(UserId.of(userId));
        } catch (UserNotFoundException exception) {
            return null;
        }
    }

    private LoginResponse toLoginResponse(User user) {
        return new LoginResponse(
                user.getId().getValue(),
                user.getDisplayName(),
                user.getEmail().getValue(),
                user.getRole().name(),
                user.isPasswordChangeRequired());
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
