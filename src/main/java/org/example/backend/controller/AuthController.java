package org.example.backendkelaspbo.controller;

import jakarta.validation.Valid;
import org.example.backendkelaspbo.dto.AuthResponse;
import org.example.backendkelaspbo.dto.LoginRequest;
import org.example.backendkelaspbo.dto.RegisterRequest;
import org.example.backendkelaspbo.dto.UpdateColorRequest;
import org.example.backendkelaspbo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Update warna terakhir — butuh token */
    @PutMapping("/me/color")
    public ResponseEntity<Void> updateColor(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateColorRequest request) {
        authService.updateLastColor(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    /** Ambil data user saat ini — butuh token */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getMe(userDetails.getUsername()));
    }
}
