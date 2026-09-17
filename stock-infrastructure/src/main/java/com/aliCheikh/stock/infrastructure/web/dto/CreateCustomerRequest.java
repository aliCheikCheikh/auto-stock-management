package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Customer registration request. The domain normalizes the phone number; father name and email are
 * optional.
 */
public record CreateCustomerRequest(@NotBlank @Size(max = 100) String givenName,
                                    @Size(max = 100) String fatherName,
                                    @NotBlank @Size(max = 30) String phoneNumber,
                                    @Email @Size(max = 200) String email) {
}
