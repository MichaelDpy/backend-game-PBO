package org.example.backend.dto;

import org.example.backend.enums.RoomStatus;

import java.util.List;

public record RoomDto(
        Long id,
        String code,
        RoomStatus status,
        int currentRound,
        List<PlayerDto> players,
        Long myPlayerId
) {}
