package org.example.backend.dto;

import org.example.backend.enums.PowerUpType;

public record PowerUpEventDto(
        Long playerId,
        PowerUpType type,
        int posX,
        int posY,
        boolean autoActivated
) {}
