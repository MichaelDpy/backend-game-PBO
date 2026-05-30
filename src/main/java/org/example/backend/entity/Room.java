package org.example.backend.entity;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.example.backend.enums.RoomStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Mewarisi BaseEntity (INHERITANCE).
 * Field private dengan getter/setter (ENCAPSULATION).
 */
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.WAITING;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();

    @Column(nullable = false)
    private int currentRound = 0;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime finishedAt;  // dicatat saat status berubah ke FINISHED

    // Constructor
    public Room(String code) {
        this.code = code;
        this.status = RoomStatus.WAITING;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(5);
    }

    // Getter & Setter
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    /** Tandai room sebagai selesai dan catat waktu selesainya */
    public void markFinished() {
        this.status = RoomStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }




    // Required by JPA
    public Room() {}


    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }

    /** Mengembalikan host room */
    public Player getHost() {
        return players.stream()
                .filter(Player::isHost)
                .findFirst()
                .orElse(null);
    }

    public boolean isFull() {
        return players.size() >= 4;
    }

    public boolean isAvailable() {
        return status == RoomStatus.WAITING && !isFull();
    }
}
