package org.example.backendkelaspbo.service;

import org.example.backendkelaspbo.config.JwtUtil;
import org.example.backendkelaspbo.dto.AuthResponse;
import org.example.backendkelaspbo.dto.LoginRequest;
import org.example.backendkelaspbo.dto.RegisterRequest;
import org.example.backendkelaspbo.dto.UpdateColorRequest;
import org.example.backendkelaspbo.entity.UserAccount;
import org.example.backendkelaspbo.enums.MowerColor;
import org.example.backendkelaspbo.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new IllegalStateException("Username sudah digunakan");
        }

        UserAccount account = new UserAccount(
                request.username(),
                passwordEncoder.encode(request.password())
        );
        account = userAccountRepository.save(account);

        String token = jwtUtil.generateToken(account.getUsername());
        return new AuthResponse(account.getId(), account.getUsername(), account.getLastColor(), token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Username atau password salah"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Username atau password salah");
        }

        String token = jwtUtil.generateToken(account.getUsername());
        return new AuthResponse(account.getId(), account.getUsername(), account.getLastColor(), token);
    }

    @Transactional
    public void updateLastColor(String username, UpdateColorRequest request) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        account.setLastColor(request.color());
        userAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public AuthResponse getMe(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        // Token tidak perlu di-regenerate untuk getMe
        return new AuthResponse(account.getId(), account.getUsername(), account.getLastColor(), null);
    }
}
