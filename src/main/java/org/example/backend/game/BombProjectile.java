package org.example.backend.game;

/**
 * Representasi bom yang sedang terbang di udara.
 */
public class BombProjectile {

    private final Long throwerPlayerId;
    private final Long targetPlayerId;
    private final int fromX;
    private final int fromY;
    private final int toX;
    private final int toY;
    private final long launchTime;
    private final long arrivalTime;

    public BombProjectile(Long throwerPlayerId, Long targetPlayerId,
                          int fromX, int fromY, int toX, int toY, int flightDurationMs) {
        this.throwerPlayerId = throwerPlayerId;
        this.targetPlayerId = targetPlayerId;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.launchTime = System.currentTimeMillis();
        this.arrivalTime = launchTime + flightDurationMs;
    }

    public Long getThrowerPlayerId() { return throwerPlayerId; }
    public Long getTargetPlayerId() { return targetPlayerId; }
    public int getFromX() { return fromX; }
    public int getFromY() { return fromY; }
    public int getToX() { return toX; }
    public int getToY() { return toY; }
    public long getLaunchTime() { return launchTime; }
    public long getArrivalTime() { return arrivalTime; }

    public boolean hasArrived() {
        return System.currentTimeMillis() >= arrivalTime;
    }

    /** Progress animasi 0.0 - 1.0 */
    public double getProgress() {
        long now = System.currentTimeMillis();
        long duration = arrivalTime - launchTime;
        return Math.min(1.0, (double)(now - launchTime) / duration);
    }
}
