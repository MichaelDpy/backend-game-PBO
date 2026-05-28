package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.backend.enums.MowerColor;

public record CreateRoomRequest(
        @NotBlank(message = "Nama pemain tidak boleh kosong")
        @Size(min = 1, max = 20, message = "Nama pemain harus 1-20 karakter")
        String playerName,
        
        @NotNull(message = "Warna harus dipilih")
        MowerColor color,
        
        String accountUsername  // null jika guest
) {}
