package org.example.backend.powerup;

import org.example.backend.enums.PowerUpType;

/**
 * Power-up Batu — diaktifkan manual dengan Space.
 * Mewarisi PowerUp (INHERITANCE), override method (POLYMORPHISM).
 */
public class RockPowerUp extends PowerUp {

    public RockPowerUp() {
        super(PowerUpType.ROCK, "rock", "Batu");
    }

    @Override
    public boolean isAutoActivate() {
        return false; // Harus tekan Space
    }

    @Override
    public int getEffectDurationMs() {
        return 0; // Batu permanen sampai ditabrak
    }
}
