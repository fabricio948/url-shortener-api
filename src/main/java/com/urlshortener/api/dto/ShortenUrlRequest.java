package com.urlshortener.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShortenUrlRequest(
        @NotBlank(message = "Name cannot be empty")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String userName,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[C|c][O|o][M|m]$",
                message = "Email must end with .com")
        String userEmail,

        @NotBlank(message = "Original URL cannot be empty")
        @Pattern(regexp = "^https?://.+",
                message = "URL must start with http:// or https://")
        String originalUrl
) {
}
