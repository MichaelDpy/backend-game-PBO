package org.example.backend.dto;

import org.example.backend.enums.GamePhase;

import java.util.List;

public record GameStateDto(
        String roomCode,
        GamePhase phase,
        int round,
        int countdownValue,          // 3, 2, 1, 0
        boolean[][] grassGrid,       // true = ada rumput
        boolean[][] rockGrid,        // true = ada batu
        List<PlayerDto> players,
        QuizStateDto quizState,      // null jika tidak sedang kuis
        String winnerId,             // null jika belum selesai
        List<BombDto> activeBombs    // bom yang sedang terbang
) {}
