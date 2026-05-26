package org.example.backend.powerup;

import org.example.backend.enums.PowerUpType;

/**
 * Abstract class untuk power-up — menerapkan ABSTRACTION.
 * Setiap power-up punya perilaku berbeda (POLYMORPHISM).
 * Field private dengan getter (ENCAPSULATION).
 */
public abstract class PowerUp {

    private final PowerUpType type;
    private final String iconName;
    private final String displayName;

    protected PowerUp(PowerUpType type, String iconName, String displayName) {
        this.type = type;
        this.iconName = iconName;
        this.displayName = displayName;
    }

    public PowerUpType getType() { return type; }
    public String getIconName() { return iconName; }
    public String getDisplayName() { return displayName; }

    /**
     * Apakah power-up ini diaktifkan otomatis (tanpa tekan Space)?
     * POLYMORPHISM — setiap subclass override sesuai kebutuhan.
     */
    public abstract boolean isAutoActivate();

    /**
     * Durasi efek dalam milidetik (0 = instan/permanen sampai dipakai).
     */
    public abstract int getEffectDurationMs();
}
