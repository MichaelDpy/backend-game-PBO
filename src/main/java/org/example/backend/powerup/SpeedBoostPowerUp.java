package org.example.backend.powerup;

import org.example.backend.enums.PowerUpType;

/**
 * Power-up Speed Boost — aktif otomatis, durasi 3 detik.
 * Mewarisi PowerUp (INHERITANCE), override method (POLYMORPHISM).
 */
public class SpeedBoostPowerUp extends PowerUp {

    public SpeedBoostPowerUp() {
        super(PowerUpType.SPEED_BOOST, "speed", "Speed Boost");
    }

    @Override
    public boolean isAutoActivate() {
        return true; // Langsung aktif
    }

    @Override
    public int getEffectDurationMs() {
        return 3000; // 3 detik
    }
}
