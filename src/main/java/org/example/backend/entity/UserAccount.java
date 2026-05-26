package org.example.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.backend.enums.MowerColor;

/**
 * Akun pengguna untuk login/register.
 * Terpisah dari entity Player — satu UserAccount bisa bermain berkali-kali.
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {

    @NotBlank
    @Size(min = 3, max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash;

    /** Warna terakhir yang dipilih pengguna */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MowerColor lastColor = MowerColor.RED;

    // ---- Stats permanen ----
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

    // Required by JPA
    public UserAccount() {}

    public UserAccount(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.lastColor = MowerColor.RED;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public MowerColor getLastColor() { return lastColor; }
    public void setLastColor(MowerColor lastColor) { this.lastColor = lastColor; }

    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(int v) { this.totalGamesPlayed = v; }

    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int v) { this.totalWins = v; }

    public int getTotalLosses() { return totalLosses; }
    public void setTotalLosses(int v) { this.totalLosses = v; }

    public int getTotalQuizAnswered() { return totalQuizAnswered; }
    public void setTotalQuizAnswered(int v) { this.totalQuizAnswered = v; }

    public int getTotalQuizCorrect() { return totalQuizCorrect; }
    public void setTotalQuizCorrect(int v) { this.totalQuizCorrect = v; }

    public int getTotalGrassCut() { return totalGrassCut; }
    public void setTotalGrassCut(int v) { this.totalGrassCut = v; }

    public int getTotalRoundsPlayed() { return totalRoundsPlayed; }
    public void setTotalRoundsPlayed(int v) { this.totalRoundsPlayed = v; }
}
