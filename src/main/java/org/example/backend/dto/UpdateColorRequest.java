package org.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import org.example.backend.enums.MowerColor;

public record UpdateColorRequest(
        @NotNull MowerColor color
) {}
