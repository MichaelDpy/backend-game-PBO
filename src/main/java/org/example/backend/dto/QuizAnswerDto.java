package org.example.backend.dto;

public record QuizAnswerDto(
        Long playerId,
        int selectedIndex
) {}
