package org.example.backend.service;

import org.example.backend.dto.*;
import java.time.ZoneId;
import org.example.backend.entity.Player;
import org.example.backend.entity.Room;
import org.example.backend.enums.RoomStatus;
import org.example.backend.repository.PlayerRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Random random = new Random();

    public RoomService(RoomRepository roomRepository,
                       PlayerRepository playerRepository,
                       SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        String code = generateUniqueCode();
        Room room = new Room(code);
        room.setCreatedAt(LocalDateTime.now());
        room.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        room = roomRepository.save(room);

        Player host = new Player(request.playerName(), request.color(), true);
        host.setRoom(room);
        if (request.accountUsername() != null) {
            host.setAccountUsername(request.accountUsername());
        }
        host = playerRepository.save(host);
        room.getPlayers().add(host);

        RoomDto dto = toDto(room, host.getId());
        broadcastRoomUpdate(room, host.getId());
        return dto;
    }

    @Transactional
    public RoomDto joinRoom(JoinRoomRequest request) {
        Room room = roomRepository.findByCodeWithPlayers(request.roomCode())
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan: " + request.roomCode()));

        if (!room.isAvailable()) {
            throw new IllegalStateException("Room penuh atau sudah dimulai");
        }

        Player player = new Player(request.playerName(), request.color(), false);
        player.setRoom(room);
        if (request.accountUsername() != null) {
            player.setAccountUsername(request.accountUsername());
        }
        player = playerRepository.save(player);
        room.getPlayers().add(player);

        RoomDto dto = toDto(room, player.getId());

        // Broadcast to everyone already in the room so they see the new player
        broadcastRoomUpdate(room, null);

        return dto;
    }

    /**
     * Setiap 60 detik:
     * 1. Expire room WAITING yang sudah melewati expiresAt → FINISHED
     * 2. Expire room WAITING yang kosong (semua player pergi) → FINISHED
     * Broadcast status FINISHED agar semua client redirect ke menu.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireStaleRooms() {
        LocalDateTime now = LocalDateTime.now();

        // Kasus 1: timer habis (expiresAt terlewat)
        List<Room> timerExpired = roomRepository.findByExpiresAtBeforeAndStatusNot(
                now, RoomStatus.FINISHED);
        for (Room room : timerExpired) {
            if (room.getStatus() == RoomStatus.PLAYING) continue;
            room.markFinished();
            roomRepository.save(room);
            broadcastRoomUpdate(room, null);
        }

        // Kasus 2: room WAITING yang kosong (tidak ada player sama sekali)
        List<Room> allWaiting = roomRepository.findByStatus(RoomStatus.WAITING);
        for (Room room : allWaiting) {
            if (room.getPlayers().isEmpty()) {
                room.markFinished();
                roomRepository.save(room);
                // Tidak perlu broadcast — tidak ada yang subscribe
            }
        }
    }

    /**
     * Setiap 6 jam: hapus permanen room FINISHED yang finishedAt-nya sudah > 3 hari.
     * Room beserta semua player-nya dihapus (cascade).
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000L)
    @Transactional
    public void deleteOldFinishedRooms() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        List<Room> oldRooms = roomRepository.findByStatusAndFinishedAtBefore(
                RoomStatus.FINISHED, cutoff);
        if (!oldRooms.isEmpty()) {
            roomRepository.deleteAll(oldRooms);
        }
    }

    @Transactional
    public void disbandRoom(String roomCode) {
        Room room = roomRepository.findByCodeWithPlayers(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan"));
        room.markFinished();
        roomRepository.save(room);
        broadcastRoomUpdate(room, null);
    }

    @Transactional(readOnly = true)
    public RoomDto getRoom(String roomCode, Long myPlayerId) {
        Room room = roomRepository.findByCodeWithPlayers(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan"));
        return toDto(room, myPlayerId);
    }

    /**
     * Broadcast current room state to all subscribers of /topic/room/{code}.
     * myPlayerId = null means broadcast a generic update (no personal myPlayerId).
     */
    public void broadcastRoomUpdate(Room room, Long myPlayerId) {
        RoomDto dto = toDto(room, myPlayerId);
        messagingTemplate.convertAndSend("/topic/room/" + room.getCode(), dto);
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
        long expiresAtMs = room.getExpiresAt() != null
                ? room.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : 0L;
        return new RoomDto(room.getId(), room.getCode(), room.getStatus(),
                room.getCurrentRound(), playerDtos, myPlayerId, expiresAtMs);
    }

    public PlayerDto toPlayerDto(Player p) {
        return new PlayerDto(p.getId(), p.getName(), p.getColor(), p.isHost(),
                2, 0, 0, 0, 0, "right", true, false, false, null, 0, false, 0L);
    }
}