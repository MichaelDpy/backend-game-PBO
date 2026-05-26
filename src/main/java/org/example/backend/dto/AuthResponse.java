package org.example.backendkelaspbo.dto;

import org.example.backendkelaspbo.enums.MowerColor;

public record AuthResponse(
        Long id,
        String username,
        MowerColor lastColor,
        String token
) {}
