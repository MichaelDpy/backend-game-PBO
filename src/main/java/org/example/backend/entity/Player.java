package org.example.backendkelaspbo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.backendkelaspbo.enums.MowerColor;
import org.example.backendkelaspbo.enums.PowerUpType;

/**
 * Mewarisi BaseEntity (INHERITANCE).
 * Field private dengan getter/setter (ENCAPSULATION).
 */
@Entity
@Table(name = "players")
public class Player extends BaseEntity {

    @NotBlank
    @Size(min = 1, max = 20)
    @Column(nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MowerColor color;

    @Column(nullable = false)
    private boolean isHost = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column
    private String sessionId;

    //Stats permanen (disimpan ke DB)
    @Column(nullable = false)
    private int totalGamesPlayed = 0;

    @Column(nullable = false)
    private int totalWins = 0;

    @Column(nullable = false)
    private int totalLosses = 0;

    @Column(nullable = false)
    private int totalQuizAnswered = 0;

    @Column(nullable = false)
    private int totalQuizCorrect = 0;

    @Column(nullable = false)
    private int totalGrassCut = 0;

    @Column(nullable = false)
    private int totalRoundsPlayed = 0;

    //State in-game (tidak disimpan ke DB)
    @Transient
    private int grassCutThisGame = 0;

    @Transient
    private int lives = 2;

    @Transient
    private int posX = 0;

    @Transient
    private int posY = 0;

    @Transient
    private String direction = "right";

    @Transient
    private boolean alive = true;

    @Transient
    private boolean crashed = false;

    @Transient
    private PowerUpType heldPowerUp = null;

    @Transient
    private boolean speedBoosted = false;

    // Required by JPA
    public Player() {}

    public Player(String name, MowerColor color, boolean isHost) {
        this.name = name;
        this.color = color;
        this.isHost = isHost;
    }

    /*getter & setter*/

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MowerColor getColor() { return color; }
    public void setColor(MowerColor color) { this.color = color; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(int totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }

    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }

    public int getTotalLosses() { return totalLosses; }
    public void setTotalLosses(int totalLosses) { this.totalLosses = totalLosses; }

    public int getTotalQuizAnswered() { return totalQuizAnswered; }
    public void setTotalQuizAnswered(int totalQuizAnswered) { this.totalQuizAnswered = totalQuizAnswered; }

    public int getTotalQuizCorrect() { return totalQuizCorrect; }
    public void setTotalQuizCorrect(int totalQuizCorrect) { this.totalQuizCorrect = totalQuizCorrect; }

    public int getTotalGrassCut() { return totalGrassCut; }
    public void setTotalGrassCut(int totalGrassCut) { this.totalGrassCut = totalGrassCut; }

    public int getTotalRoundsPlayed() { return totalRoundsPlayed; }
    public void setTotalRoundsPlayed(int totalRoundsPlayed) { this.totalRoundsPlayed = totalRoundsPlayed; }

    public int getGrassCutThisGame() { return grassCutThisGame; }
    public void setGrassCutThisGame(int grassCutThisGame) { this.grassCutThisGame = grassCutThisGame; }

    public int getLives() { return lives; }
    public void setLives(int lives) { this.lives = lives; }

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

    public PowerUpType getHeldPowerUp() { return heldPowerUp; }
    public void setHeldPowerUp(PowerUpType heldPowerUp) { this.heldPowerUp = heldPowerUp; }

    public boolean isSpeedBoosted() { return speedBoosted; }
    public void setSpeedBoosted(boolean speedBoosted) { this.speedBoosted = speedBoosted; }

    /* Kurangi nyawa, return true jika masih hidup */
    public boolean loseLife() {
        if (lives > 0) lives--;
        if (lives <= 0) alive = false;
        return alive;
    }

    /*Tambah rumput yang dipotong dalam game ini */
    public void addGrassCut(int amount) {
        grassCutThisGame += amount;
        totalGrassCut += amount;
    }
}
