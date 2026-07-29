package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameUserRequest(@NotBlank @Size(max = 100) String displayName) {
}
