package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username tidak boleh kosong")
        @Size(min = 3, max = 20, message = "Username harus 3-20 karakter")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username hanya boleh huruf, angka, dan underscore")
        String username,
        
        @NotBlank(message = "Password tidak boleh kosong")
        @Size(min = 4, max = 50, message = "Password harus 4-50 karakter")
        String password
) {}
