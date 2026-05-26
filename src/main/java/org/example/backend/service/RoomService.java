package org.example.backend.service;

import org.example.backend.dto.*;
import org.example.backend.entity.Player;
import org.example.backend.entity.Room;
import org.example.backend.enums.RoomStatus;
import org.example.backend.repository.PlayerRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * Service untuk manajemen room.
 * Menerapkan ENCAPSULATION — logika bisnis tersembunyi di dalam service.
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final Random random = new Random();

    public RoomService(RoomRepository roomRepository, PlayerRepository playerRepository) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        String code = generateUniqueCode();
        Room room = new Room(code);
        room = roomRepository.save(room);

        Player host = new Player(request.playerName(), request.color(), true);
        host.setRoom(room);
        host = playerRepository.save(host);
        room.getPlayers().add(host);

        return toDto(room, host.getId());
    }

    @Transactional
    public RoomDto joinRoom(JoinRoomRequest request) {
        Room room = roomRepository.findByCode(request.roomCode())
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan: " + request.roomCode()));

        if (!room.isAvailable()) {
            throw new IllegalStateException("Room penuh atau sudah dimulai");
        }

        Player player = new Player(request.playerName(), request.color(), false);
        player.setRoom(room);
        player = playerRepository.save(player);
        room.getPlayers().add(player);

        return toDto(room, player.getId());
    }

    @Transactional
    public void disbandRoom(String roomCode) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan"));
        room.setStatus(RoomStatus.FINISHED);
        roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public RoomDto getRoom(String roomCode, Long myPlayerId) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan"));
        return toDto(room, myPlayerId);
    }

    private String generateUniqueCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByCode(code));
        return code;
    }

    public RoomDto toDto(Room room, Long myPlayerId) {
        List<PlayerDto> playerDtos = room.getPlayers().stream()
                .map(this::toPlayerDto)
                .toList();
        return new RoomDto(room.getId(), room.getCode(), room.getStatus(),
                room.getCurrentRound(), playerDtos, myPlayerId);
    }

    public PlayerDto toPlayerDto(Player p) {
        return new PlayerDto(p.getId(), p.getName(), p.getColor(), p.isHost(),
                2, 0, 0, 0, "right", true, false, false, null);
    }
}
