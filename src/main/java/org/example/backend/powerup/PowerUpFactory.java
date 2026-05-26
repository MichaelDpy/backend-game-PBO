package org.example.backend.powerup;

import org.example.backend.enums.PowerUpType;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Factory untuk membuat PowerUp — menerapkan POLYMORPHISM.
 * Caller tidak perlu tahu tipe konkret, cukup minta PowerUp.
 */
@Component
public class PowerUpFactory {

    private final Random random = new Random();

    /**
     * Buat power-up berdasarkan tipe.
     * POLYMORPHISM — return type adalah abstract PowerUp.
     */
    public PowerUp create(PowerUpType type) {
        return switch (type) {
            case ROCK -> new RockPowerUp();
            case SPEED_BOOST -> new SpeedBoostPowerUp();
            case BOMB -> new BombPowerUp();
        };
    }

    /**
     * Buat power-up acak.
     * @param rockEnabled apakah power-up batu tersedia
     */
    public PowerUp createRandom(boolean rockEnabled) {
        PowerUpType[] available = rockEnabled
                ? PowerUpType.values()
                : new PowerUpType[]{PowerUpType.SPEED_BOOST, PowerUpType.BOMB};
        return create(available[random.nextInt(available.length)]);
    }
}
