package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Category creation or rename request. Input validation checks size and presence; the application
 * checks uniqueness.
 */
public record CategoryRequest(@NotBlank @Size(max = 255) String name) {
}
