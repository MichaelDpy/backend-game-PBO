package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username tidak boleh kosong")
        String username,
        
        @NotBlank(message = "Password tidak boleh kosong")
        String password
) {}
