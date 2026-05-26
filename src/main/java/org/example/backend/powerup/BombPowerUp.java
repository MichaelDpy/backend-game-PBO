package org.example.backend.powerup;

import org.example.backend.enums.PowerUpType;

/**
 * Power-up Bom — diaktifkan manual dengan Space, melempar ke pemain terdekat.
 * Mewarisi PowerUp (INHERITANCE), override method (POLYMORPHISM).
 */
public class BombPowerUp extends PowerUp {

    public BombPowerUp() {
        super(PowerUpType.BOMB, "bomb", "Bom");
    }

    @Override
    public boolean isAutoActivate() {
        return false; // Harus tekan Space
    }

    @Override
    public int getEffectDurationMs() {
        return 1500; // Animasi terbang 1.5 detik
    }
}
