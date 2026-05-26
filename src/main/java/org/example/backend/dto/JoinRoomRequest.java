package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.backend.enums.MowerColor;

public record JoinRoomRequest(
        @NotBlank @Size(min = 1, max = 20) String playerName,
        @NotNull MowerColor color,
        @NotBlank String roomCode
) {}
