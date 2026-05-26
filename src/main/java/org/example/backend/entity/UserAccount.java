package org.example.backendkelaspbo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.backendkelaspbo.enums.MowerColor;

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
}
