package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.CreateSellerCommand;
import com.aliCheikh.stock.application.dto.CreatedUser;
import com.aliCheikh.stock.application.dto.TemporaryPassword;
import com.aliCheikh.stock.application.usecase.CreateSellerUseCase;
import com.aliCheikh.stock.application.usecase.DeactivateUserUseCase;
import com.aliCheikh.stock.application.usecase.ListUsersUseCase;
import com.aliCheikh.stock.application.usecase.ReactivateUserUseCase;
import com.aliCheikh.stock.application.usecase.RenameUserUseCase;
import com.aliCheikh.stock.application.usecase.ResetSellerPasswordUseCase;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.web.dto.CreateUserRequest;
import com.aliCheikh.stock.infrastructure.web.dto.CreatedUserResponse;
import com.aliCheikh.stock.infrastructure.web.dto.RenameUserRequest;
import com.aliCheikh.stock.infrastructure.web.dto.TemporaryPasswordResponse;
import com.aliCheikh.stock.infrastructure.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CreateSellerUseCase createSellerUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final RenameUserUseCase renameUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final ReactivateUserUseCase reactivateUserUseCase;
    private final ResetSellerPasswordUseCase resetSellerPasswordUseCase;

    public UserController(CreateSellerUseCase createSellerUseCase,
                          ListUsersUseCase listUsersUseCase,
                          RenameUserUseCase renameUserUseCase,
                          DeactivateUserUseCase deactivateUserUseCase,
                          ReactivateUserUseCase reactivateUserUseCase,
                          ResetSellerPasswordUseCase resetSellerPasswordUseCase) {
        this.createSellerUseCase = Objects.requireNonNull(createSellerUseCase);
        this.listUsersUseCase = Objects.requireNonNull(listUsersUseCase);
        this.renameUserUseCase = Objects.requireNonNull(renameUserUseCase);
        this.deactivateUserUseCase = Objects.requireNonNull(deactivateUserUseCase);
        this.reactivateUserUseCase = Objects.requireNonNull(reactivateUserUseCase);
        this.resetSellerPasswordUseCase = Objects.requireNonNull(resetSellerPasswordUseCase);
    }

    @PostMapping
    public ResponseEntity<CreatedUserResponse> createSeller(@Valid @RequestBody CreateUserRequest request) {
        CreatedUser created = createSellerUseCase.execute(
                new CreateSellerCommand(request.displayName(), request.email()));
        CreatedUserResponse response = new CreatedUserResponse(
                UserResponse.from(created.user()),
                created.temporaryPassword());

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return listUsersUseCase.execute().stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/{userId}/display-name")
    public UserResponse renameUser(@PathVariable UUID userId,
                                   @Valid @RequestBody RenameUserRequest request) {
        User renamed = renameUserUseCase.execute(UserId.of(userId), request.displayName());
        return UserResponse.from(renamed);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
        deactivateUserUseCase.execute(UserId.of(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reactivate")
    public UserResponse reactivateUser(@PathVariable UUID userId) {
        return UserResponse.from(reactivateUserUseCase.execute(UserId.of(userId)));
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<TemporaryPasswordResponse> resetPassword(@PathVariable UUID userId) {
        TemporaryPassword temporaryPassword = resetSellerPasswordUseCase.execute(UserId.of(userId));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TemporaryPasswordResponse(temporaryPassword.value()));
    }
}
