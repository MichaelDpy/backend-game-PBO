package org.example.backend.dto;

/**
 * Input dari pemain: arah gerak atau aksi (space = aktifkan power-up).
 */
public record PlayerInputDto(
        Long playerId,
        String direction,   // "up", "down", "left", "right", null
        boolean activatePowerUp  // true jika tekan Space
) {}
