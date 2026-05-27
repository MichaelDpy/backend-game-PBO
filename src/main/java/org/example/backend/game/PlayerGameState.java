package org.example.backend.game;

import org.example.backend.enums.PowerUpType;

/**
 * State in-game satu pemain — ENCAPSULATION.
 */
public class PlayerGameState {

    private final Long playerId;
    private int posX;
    private int posY;
    private String direction = "right";
    private boolean alive = true;
    private boolean crashed = false;
    private int lives = 2;
    private int grassCutThisRound = 0;
    private int grassCutTotal = 0;
    private PowerUpType heldPowerUp = null;
    private boolean speedBoosted = false;
    private long speedBoostEndTime = 0;
    // How many rounds this player survived (incremented at end of each round they were alive)
    private int roundsSurvived = 0;

    public PlayerGameState(Long playerId, int startX, int startY, String startDir) {
        this.playerId = playerId;
        this.posX = startX;
        this.posY = startY;
        this.direction = startDir;
    }

    public void resetForRound() {
        crashed = false;
        heldPowerUp = null;
        speedBoosted = false;
        speedBoostEndTime = 0;
        grassCutThisRound = 0;
    }

    public boolean loseLife() {
        if (lives > 0) lives--;
        if (lives <= 0) alive = false;
        return alive;
    }

    public void addGrassCut() {
        grassCutThisRound++;
        grassCutTotal++;
    }

    public void activateSpeedBoost(int durationMs) {
        speedBoosted = true;
        speedBoostEndTime = System.currentTimeMillis() + durationMs;
    }

    // ---- Getters ----

    public Long getPlayerId() { return playerId; }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }

    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public boolean isCrashed() { return crashed; }
    public void setCrashed(boolean crashed) { this.crashed = crashed; }

    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }

    public int getGrassCutThisRound() { return grassCutThisRound; }
    public void setGrassCutThisRound(int grassCutThisRound) { this.grassCutThisRound = grassCutThisRound; }

    public int getGrassCutTotal() { return grassCutTotal; }
    public void setGrassCutTotal(int grassCutTotal) { this.grassCutTotal = grassCutTotal; }

    public int getRoundsSurvived() { return roundsSurvived; }
    public void setRoundsSurvived(int roundsSurvived) { this.roundsSurvived = roundsSurvived; }
    public void incrementRoundsSurvived() { this.roundsSurvived++; }

    public PowerUpType getHeldPowerUp() { return heldPowerUp; }
    public void setHeldPowerUp(PowerUpType heldPowerUp) { this.heldPowerUp = heldPowerUp; }

    /** Custom getter — checks expiry before returning */
    public boolean isSpeedBoosted() {
        if (speedBoosted && System.currentTimeMillis() > speedBoostEndTime) {
            speedBoosted = false;
        }
        return speedBoosted;
    }
    public void setSpeedBoosted(boolean speedBoosted) { this.speedBoosted = speedBoosted; }

    public long getSpeedBoostEndTime() { return speedBoostEndTime; }
    public void setSpeedBoostEndTime(long speedBoostEndTime) { this.speedBoostEndTime = speedBoostEndTime; }
}
