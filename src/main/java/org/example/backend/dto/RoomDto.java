package org.example.backend.dto;

import org.example.backend.enums.RoomStatus;

import java.time.Instant;
import java.util.List;

public record RoomDto(
        Long id,
        String code,
        RoomStatus status,
        int currentRound,
        List<PlayerDto> players,
        Long myPlayerId,
        long expiresAt   // epoch millis — mudah dipakai di Date.now() frontend
) {}
