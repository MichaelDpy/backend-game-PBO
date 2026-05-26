package org.example.backendkelaspbo.dto;

import jakarta.validation.constraints.NotNull;
import org.example.backendkelaspbo.enums.MowerColor;

public record UpdateColorRequest(
        @NotNull MowerColor color
) {}
