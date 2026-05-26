package org.example.backend.dto;

public record BombDto(
        Long throwerPlayerId,
        Long targetPlayerId,
        int fromX,
        int fromY,
        int toX,
        int toY,
        long launchTime,
        long arrivalTime
) {}
