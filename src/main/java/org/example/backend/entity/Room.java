package org.example.backend.entity;

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

    // Required by JPA
    public Room() {}

    public Room(String code) {
        this.code = code;
        this.status = RoomStatus.WAITING;
    }

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
