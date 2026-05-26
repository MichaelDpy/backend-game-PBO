package org.example.backend.dto;

public record StatsDto(
        Long playerId,
        String playerName,
        int totalGamesPlayed,
        int totalWins,
        int totalLosses,
        int totalQuizAnswered,
        int totalQuizCorrect,
        int totalGrassCut,
        int totalRoundsPlayed,
        double winRate,
        double quizAccuracy
) {}
