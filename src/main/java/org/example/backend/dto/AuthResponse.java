package org.example.backend.dto;

import org.example.backend.enums.MowerColor;

public record AuthResponse(
        Long id,
        String username,
        MowerColor lastColor,
        String token
) {}
