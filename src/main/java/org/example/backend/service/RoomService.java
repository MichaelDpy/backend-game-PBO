package org.example.backend.service;

import org.example.backend.dto.*;
import org.example.backend.entity.Player;
import org.example.backend.entity.Room;
import org.example.backend.enums.RoomStatus;
import org.example.backend.repository.PlayerRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void disbandRoom(String roomCode) {
        Room room = roomRepository.findByCodeWithPlayers(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan"));
        room.setStatus(RoomStatus.FINISHED);
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
        return new RoomDto(room.getId(), room.getCode(), room.getStatus(),
                room.getCurrentRound(), playerDtos, myPlayerId);
    }

    public PlayerDto toPlayerDto(Player p) {
        return new PlayerDto(p.getId(), p.getName(), p.getColor(), p.isHost(),
                2, 0, 0, 0, 0, "right", true, false, false, null, 0, false, 0L);
    }
}
