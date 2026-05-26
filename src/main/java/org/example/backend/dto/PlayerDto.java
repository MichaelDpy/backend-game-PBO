package org.example.backend.dto;

import org.example.backend.enums.MowerColor;
import org.example.backend.enums.PowerUpType;

public record PlayerDto(
        Long id,
        String name,
        MowerColor color,
        boolean isHost,
        int lives,
        int grassCut,
        int posX,
        int posY,
        String direction,
        boolean alive,
        boolean crashed,
        boolean speedBoosted,
        PowerUpType heldPowerUp
) {}
