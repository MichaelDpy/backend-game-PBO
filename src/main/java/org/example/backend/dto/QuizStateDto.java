package org.example.backend.dto;

import java.util.List;

public record QuizStateDto(
        Long targetPlayerId,
        String question,
        List<String> choices,
        int timeRemainingMs,
        Boolean answered,       // null = belum, true = benar, false = salah
        Integer selectedIndex   // null jika belum jawab
) {}
