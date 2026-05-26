package org.example.backend.service;

import org.example.backend.dto.*;
import org.example.backend.entity.Player;
import org.example.backend.entity.Room;
import org.example.backend.enums.GamePhase;
import org.example.backend.enums.PowerUpType;
import org.example.backend.enums.RoomStatus;
import org.example.backend.game.BombProjectile;
import org.example.backend.game.GameSession;
import org.example.backend.game.PlayerGameState;
import org.example.backend.powerup.PowerUp;
import org.example.backend.powerup.PowerUpFactory;
import org.example.backend.quiz.QuizBank;
import org.example.backend.quiz.QuizQuestion;
import org.example.backend.repository.PlayerRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service utama logika game.
 * Menerapkan semua 4 pilar OOP:
 * - ENCAPSULATION: state tersembunyi, akses via method
 * - INHERITANCE: menggunakan entity yang mewarisi BaseEntity
 * - ABSTRACTION: menggunakan QuizQuestion interface & PowerUp abstract class
 * - POLYMORPHISM: PowerUpFactory return PowerUp, QuizBank return QuizQuestion
 */
@Service
public class GameService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final QuizBank quizBank;
    private final PowerUpFactory powerUpFactory;
    private final Random random = new Random();
    
    public GameService(RoomRepository roomRepository,
                       PlayerRepository playerRepository,
                       SimpMessagingTemplate messagingTemplate,
                       QuizBank quizBank,
                       PowerUpFactory powerUpFactory) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.messagingTemplate = messagingTemplate;
        this.quizBank = quizBank;
        this.powerUpFactory = powerUpFactory;
    }

    // In-memory game sessions per room
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    // ===================== START GAME =====================

    public void startGame(String roomCode) {
        Room room = getRoom(roomCode);
        room.setStatus(RoomStatus.PLAYING);
        room.setCurrentRound(1);
        roomRepository.save(room);

        GameSession session = new GameSession(roomCode);
        sessions.put(roomCode, session);

        spawnPlayers(session, room.getPlayers());
        broadcastState(session, room);
        startCountdown(session, room);
    }

    private void spawnPlayers(GameSession session, List<Player> players) {
        // Posisi spawn yang tidak berdekatan
        List<int[]> spawnPoints = generateSpawnPoints(players.size());
        String[] dirs = {"right", "left", "down", "up"};

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int[] pos = spawnPoints.get(i);
            PlayerGameState state = new PlayerGameState(p.getId(), pos[0], pos[1], dirs[i % 4]);
            session.getPlayerStates().put(p.getId(), state);
        }
    }

    private List<int[]> generateSpawnPoints(int count) {
        int size = GameSession.GRID_SIZE;
        // Sudut-sudut grid dengan jarak aman
        List<int[]> corners = new ArrayList<>(List.of(
                new int[]{1, 1},
                new int[]{size - 2, 1},
                new int[]{1, size - 2},
                new int[]{size - 2, size - 2}
        ));
        Collections.shuffle(corners, random);
        return corners.subList(0, Math.min(count, corners.size()));
    }

    // ===================== COUNTDOWN =====================

    private void startCountdown(GameSession session, Room room) {
        session.setPhase(GamePhase.COUNTDOWN);
        session.setCountdownValue(3);
        broadcastState(session, room);

        // Countdown dihandle oleh @Scheduled tick
    }

    // ===================== GAME TICK (100ms) =====================

    @Scheduled(fixedRate = 100)
    public void gameTick() {
        for (Map.Entry<String, GameSession> entry : sessions.entrySet()) {
            String roomCode = entry.getKey();
            GameSession session = entry.getValue();

            try {
                Room room = getRoom(roomCode);
                if (room.getStatus() != RoomStatus.PLAYING) continue;

                switch (session.getPhase()) {
                    case COUNTDOWN -> tickCountdown(session, room);
                    case PLAYING -> tickPlaying(session, room);
                    case QUIZ -> tickQuiz(session, room);
                    default -> { /* ROUND_END, GAME_OVER handled elsewhere */ }
                }
            } catch (Exception e) {
                // Log error tapi jangan crash seluruh tick
                System.err.println("Error in game tick for room " + roomCode + ": " + e.getMessage());
            }
        }
    }

    private long lastCountdownTick = 0;

    private void tickCountdown(GameSession session, Room room) {
        long now = System.currentTimeMillis();
        if (now - lastCountdownTick < 1000) return;
        lastCountdownTick = now;

        int val = session.getCountdownValue();
        if (val > 0) {
            session.setCountdownValue(val - 1);
            broadcastState(session, room);
        } else {
            session.setPhase(GamePhase.PLAYING);
            broadcastState(session, room);
        }
    }

    // ===================== PLAYER INPUT =====================

    public void handlePlayerInput(String roomCode, PlayerInputDto input) {
        GameSession session = sessions.get(roomCode);
        if (session == null || session.getPhase() != GamePhase.PLAYING) return;

        PlayerGameState state = session.getPlayerStates().get(input.playerId());
        if (state == null || !state.isAlive() || state.isCrashed()) return;

        // Update arah
        if (input.direction() != null && !input.direction().isBlank()) {
            String newDir = input.direction();
            String cur = state.getDirection();
            // Tidak boleh balik arah 180 derajat
            if (!isOpposite(newDir, cur)) {
                state.setDirection(newDir);
            }
        }

        // Aktifkan power-up
        if (input.activatePowerUp() && state.getHeldPowerUp() != null) {
            activatePowerUp(roomCode, session, state);
        }
    }

    private boolean isOpposite(String a, String b) {
        return (a.equals("up") && b.equals("down")) ||
               (a.equals("down") && b.equals("up")) ||
               (a.equals("left") && b.equals("right")) ||
               (a.equals("right") && b.equals("left"));
    }

    // ===================== MOVEMENT TICK =====================

    private final Map<String, Long> lastMoveTick = new ConcurrentHashMap<>();

    private void tickPlaying(GameSession session, Room room) {
        long now = System.currentTimeMillis();
        String roomCode = session.getRoomCode();
        long lastMove = lastMoveTick.getOrDefault(roomCode, 0L);

        // Gerak setiap 200ms (speed boost = 120ms)
        boolean anySpeedBoosted = session.getPlayerStates().values().stream()
                .anyMatch(PlayerGameState::isSpeedBoosted);
        int moveInterval = anySpeedBoosted ? 120 : 200;

        if (now - lastMove < moveInterval) return;
        lastMoveTick.put(roomCode, now);

        // Gerakkan semua pemain
        for (PlayerGameState state : session.getPlayerStates().values()) {
            if (!state.isAlive() || state.isCrashed()) continue;
            movePlayer(session, state);
        }

        // Proses bom yang sudah sampai
        processBombs(session);

        // Cek apakah semua rumput sudah dipotong
        if (session.isAllGrassCut()) {
            endRound(session, room);
            return;
        }

        // Cek apakah hanya 1 pemain tersisa
        if (session.countAlivePlayers() <= 1) {
            endGame(session, room);
            return;
        }

        broadcastState(session, room);
    }

    private void movePlayer(GameSession session, PlayerGameState state) {
        int x = state.getPosX();
        int y = state.getPosY();
        String dir = state.getDirection();

        int newX = x, newY = y;
        switch (dir) {
            case "up" -> newY = y - 1;
            case "down" -> newY = y + 1;
            case "left" -> newX = x - 1;
            case "right" -> newX = x + 1;
        }

        // Cek batas grid
        if (newX < 0 || newX >= GameSession.GRID_SIZE ||
            newY < 0 || newY >= GameSession.GRID_SIZE) {
            // Nabrak tembok
            state.setCrashed(true);
            state.setAlive(false);
            return;
        }

        // Cek batu
        if (session.isCellBlocked(newX, newY)) {
            state.setCrashed(true);
            state.setAlive(false);
            return;
        }

        // Cek tabrakan dengan pemain lain
        Optional<Long> otherPlayer = session.getPlayerAtCell(newX, newY);
        if (otherPlayer.isPresent()) {
            // Yang menabrak hancur, yang ditabrak aman
            state.setCrashed(true);
            state.setAlive(false);
            return;
        }

        // Gerak berhasil
        state.setPosX(newX);
        state.setPosY(newY);

        // Potong rumput
        if (session.getGrassGrid()[newY][newX]) {
            session.getGrassGrid()[newY][newX] = false;
            state.addGrassCut();

            // Kemungkinan dapat power-up
            // Power-up bisa didapat kapan saja (replace yang lama jika ada),
            // kecuali jika sedang speed boost aktif — tetap bisa dapat power-up manual baru.
            // Speed boost tidak menghalangi pickup power-up lain.
            boolean canPickup = random.nextDouble() < session.getPowerUpChance();
            if (canPickup) {
                PowerUp pu = powerUpFactory.createRandom(session.isRockPowerUpEnabled());

                // Speed boost langsung aktif, tidak disimpan di slot
                if (pu.isAutoActivate()) {
                    state.activateSpeedBoost(pu.getEffectDurationMs());
                    // Jangan replace held power-up yang ada — speed boost tidak pakai slot
                    broadcastPowerUpEvent(session.getRoomCode(), state.getPlayerId(),
                            pu.getType(), newX, newY, true);
                } else {
                    // Replace power-up lama (jika ada) dengan yang baru
                    state.setHeldPowerUp(pu.getType());
                    broadcastPowerUpEvent(session.getRoomCode(), state.getPlayerId(),
                            pu.getType(), newX, newY, false);
                }
            }
        }
    }

    // ===================== POWER-UP ACTIVATION =====================

    private void activatePowerUp(String roomCode, GameSession session, PlayerGameState state) {
        PowerUpType type = state.getHeldPowerUp();
        if (type == null) return;
        state.setHeldPowerUp(null);

        switch (type) {
            case ROCK -> placeRock(session, state);
            case BOMB -> throwBomb(session, state);
            case SPEED_BOOST -> state.activateSpeedBoost(3000); // fallback
        }
    }

    private void placeRock(GameSession session, PlayerGameState state) {
        if (!session.isRockPowerUpEnabled()) return;
        // Taruh batu di belakang pemain
        int bx = state.getPosX(), by = state.getPosY();
        switch (state.getDirection()) {
            case "up" -> by += 1;
            case "down" -> by -= 1;
            case "left" -> bx += 1;
            case "right" -> bx -= 1;
        }
        if (bx >= 0 && bx < GameSession.GRID_SIZE && by >= 0 && by < GameSession.GRID_SIZE) {
            session.getPlayerRockGrid()[by][bx] = true;
        }
    }

    private void throwBomb(GameSession session, PlayerGameState thrower) {
        // Cari pemain terdekat yang masih hidup
        Optional<PlayerGameState> target = session.getPlayerStates().values().stream()
                .filter(s -> !s.getPlayerId().equals(thrower.getPlayerId())
                        && s.isAlive() && !s.isCrashed())
                .min(Comparator.comparingDouble(s ->
                        Math.hypot(s.getPosX() - thrower.getPosX(),
                                   s.getPosY() - thrower.getPosY())));

        target.ifPresent(t -> {
            BombProjectile bomb = new BombProjectile(
                    thrower.getPlayerId(), t.getPlayerId(),
                    thrower.getPosX(), thrower.getPosY(),
                    t.getPosX(), t.getPosY(), 1500);
            session.getActiveBombs().add(bomb);
        });
    }

    private void processBombs(GameSession session) {
        Iterator<BombProjectile> it = session.getActiveBombs().iterator();
        while (it.hasNext()) {
            BombProjectile bomb = it.next();
            if (bomb.hasArrived()) {
                PlayerGameState target = session.getPlayerStates().get(bomb.getTargetPlayerId());
                if (target != null && target.isAlive() && !target.isCrashed()) {
                    target.setCrashed(true);
                    target.setAlive(false);
                }
                it.remove();
            }
        }
    }

    // ===================== QUIZ =====================

    private void tickQuiz(GameSession session, Room room) {
        if (session.getQuizTargetPlayerId() == null) return;

        long elapsed = System.currentTimeMillis() - session.getQuizStartTime();
        if (elapsed >= 10000 && session.getQuizAnswered() == null) {
            // Waktu habis — salah
            handleQuizResult(session, room, false);
        }
    }

    public void handleQuizAnswer(String roomCode, QuizAnswerDto answer) {
        GameSession session = sessions.get(roomCode);
        if (session == null || session.getPhase() != GamePhase.QUIZ) return;
        if (!answer.playerId().equals(session.getQuizTargetPlayerId())) return;
        if (session.getQuizAnswered() != null) return; // sudah dijawab

        boolean correct = session.getActiveQuestion().isCorrect(answer.selectedIndex());
        session.setQuizAnswered(correct);
        session.setQuizSelectedIndex(answer.selectedIndex());

        Room room = getRoom(roomCode);

        // Update stats
        playerRepository.findById(answer.playerId()).ifPresent(p -> {
            p.setTotalQuizAnswered(p.getTotalQuizAnswered() + 1);
            if (correct) p.setTotalQuizCorrect(p.getTotalQuizCorrect() + 1);
            playerRepository.save(p);
        });

        broadcastState(session, room);

        // Tunggu 2 detik agar semua lihat hasilnya, lalu lanjut
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            handleQuizResult(session, room, correct);
        }).start();
    }

    private void handleQuizResult(GameSession session, Room room, boolean correct) {
        if (!correct) {
            PlayerGameState state = session.getPlayerStates().get(session.getQuizTargetPlayerId());
            if (state != null) {
                state.loseLife();
            }
        }

        // Lanjut ke ronde berikutnya
        startNextRound(session, room);
    }

    // ===================== ROUND END =====================

    private void endRound(GameSession session, Room room) {
        session.setPhase(GamePhase.QUIZ);

        // Pemain dengan rumput paling sedikit kena kuis
        Optional<Long> loser = session.getLowestScoringAlivePlayer();
        if (loser.isEmpty()) {
            startNextRound(session, room);
            return;
        }

        session.setQuizTargetPlayerId(loser.get());
        session.setActiveQuestion(quizBank.getRandom());
        session.setQuizStartTime(System.currentTimeMillis());
        session.setQuizAnswered(null);
        session.setQuizSelectedIndex(null);

        broadcastState(session, room);
    }

    private void startNextRound(GameSession session, Room room) {
        int newRound = session.getRound() + 1;
        session.setRound(newRound);
        room.setCurrentRound(newRound);
        roomRepository.save(room);

        // Tambah batu penghalang setiap kelipatan 3
        addObstacleRocksIfNeeded(session, newRound);

        session.resetForNewRound();
        spawnPlayers(session, room.getPlayers().stream()
                .filter(p -> {
                    PlayerGameState s = session.getPlayerStates().get(p.getId());
                    return s != null && s.isAlive();
                }).toList());

        broadcastState(session, room);
        startCountdown(session, room);
    }

    private void addObstacleRocksIfNeeded(GameSession session, int round) {
        // Ronde 4 (kelipatan 3 + 1): tambah 5 batu
        // Ronde 7, 10, 13 (kelipatan 3 + 1): tambah 2 batu
        if (round == 4) {
            for (int i = 0; i < 5; i++) {
                boolean added = session.addObstacleRock(random);
                if (!added) {
                    session.setRockPowerUpEnabled(false);
                    break;
                }
            }
            // Naikkan probabilitas power-up
            session.setPowerUpChance(session.getPowerUpChance() + 0.05);
        } else if (round > 4 && (round - 1) % 3 == 0) {
            for (int i = 0; i < 2; i++) {
                boolean added = session.addObstacleRock(random);
                if (!added) {
                    session.setRockPowerUpEnabled(false);
                    break;
                }
            }
            session.setPowerUpChance(Math.min(0.5, session.getPowerUpChance() + 0.03));
        }
    }

    // ===================== GAME OVER =====================

    private void endGame(GameSession session, Room room) {
        session.setPhase(GamePhase.GAME_OVER);

        // Temukan pemenang (pemain yang masih hidup)
        Optional<PlayerGameState> winner = session.getPlayerStates().values().stream()
                .filter(s -> s.isAlive() && !s.isCrashed())
                .findFirst();

        winner.ifPresent(w -> {
            session.setWinnerId(String.valueOf(w.getPlayerId()));
            // Update stats pemenang
            playerRepository.findById(w.getPlayerId()).ifPresent(p -> {
                p.setTotalWins(p.getTotalWins() + 1);
                p.setTotalGamesPlayed(p.getTotalGamesPlayed() + 1);
                p.setTotalRoundsPlayed(p.getTotalRoundsPlayed() + session.getRound());
                p.setTotalGrassCut(p.getTotalGrassCut() + w.getGrassCutTotal());
                playerRepository.save(p);
            });
        });

        // Update stats semua pemain yang kalah
        session.getPlayerStates().values().stream()
                .filter(s -> !s.isAlive() || s.isCrashed())
                .forEach(s -> playerRepository.findById(s.getPlayerId()).ifPresent(p -> {
                    p.setTotalLosses(p.getTotalLosses() + 1);
                    p.setTotalGamesPlayed(p.getTotalGamesPlayed() + 1);
                    p.setTotalRoundsPlayed(p.getTotalRoundsPlayed() + session.getRound());
                    p.setTotalGrassCut(p.getTotalGrassCut() + s.getGrassCutTotal());
                    playerRepository.save(p);
                }));

        room.setStatus(RoomStatus.FINISHED);
        roomRepository.save(room);

        broadcastState(session, room);
    }

    public void retryGame(String roomCode) {
        Room room = getRoom(roomCode);
        room.setStatus(RoomStatus.PLAYING);
        room.setCurrentRound(1);
        roomRepository.save(room);

        // Reset semua player stats in-game
        room.getPlayers().forEach(p -> {
            p.setTotalGamesPlayed(p.getTotalGamesPlayed()); // sudah di-update di endGame
        });

        GameSession session = new GameSession(roomCode);
        sessions.put(roomCode, session);
        spawnPlayers(session, room.getPlayers());
        broadcastState(session, room);
        startCountdown(session, room);
    }

    // ===================== BROADCAST =====================

    private void broadcastState(GameSession session, Room room) {
        GameStateDto state = buildGameState(session, room);
        messagingTemplate.convertAndSend("/topic/room/" + session.getRoomCode(), state);
    }

    private void broadcastPowerUpEvent(String roomCode, Long playerId, PowerUpType type,
                                        int x, int y, boolean autoActivated) {
        PowerUpEventDto event = new PowerUpEventDto(playerId, type, x, y, autoActivated);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/powerup", event);
    }

    public GameStateDto buildGameState(GameSession session, Room room) {
        List<PlayerDto> playerDtos = room.getPlayers().stream()
                .map(p -> {
                    PlayerGameState s = session.getPlayerStates().get(p.getId());
                    if (s == null) {
                        return new PlayerDto(p.getId(), p.getName(), p.getColor(), p.isHost(),
                                2, 0, 0, 0, "right", true, false, false, null);
                    }
                    return new PlayerDto(p.getId(), p.getName(), p.getColor(), p.isHost(),
                            s.getLives(), s.getGrassCutTotal(),
                            s.getPosX(), s.getPosY(), s.getDirection(),
                            s.isAlive(), s.isCrashed(), s.isSpeedBoosted(), s.getHeldPowerUp());
                }).toList();

        QuizStateDto quizState = null;
        if (session.getPhase() == GamePhase.QUIZ && session.getActiveQuestion() != null) {
            long elapsed = System.currentTimeMillis() - session.getQuizStartTime();
            int remaining = (int) Math.max(0, 10000 - elapsed);
            quizState = new QuizStateDto(
                    session.getQuizTargetPlayerId(),
                    session.getActiveQuestion().getQuestion(),
                    session.getActiveQuestion().getChoices(),
                    remaining,
                    session.getQuizAnswered(),
                    session.getQuizSelectedIndex()
            );
        }

        return new GameStateDto(
                session.getRoomCode(),
                session.getPhase(),
                session.getRound(),
                session.getCountdownValue(),
                session.getGrassGrid(),
                session.getRockGrid(),
                playerDtos,
                quizState,
                session.getWinnerId()
        );
    }

    // ===================== STATS =====================

    public StatsDto getStats(Long playerId) {
        Player p = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player tidak ditemukan"));
        double winRate = p.getTotalGamesPlayed() == 0 ? 0 :
                (double) p.getTotalWins() / p.getTotalGamesPlayed() * 100;
        double quizAcc = p.getTotalQuizAnswered() == 0 ? 0 :
                (double) p.getTotalQuizCorrect() / p.getTotalQuizAnswered() * 100;
        return new StatsDto(p.getId(), p.getName(), p.getTotalGamesPlayed(),
                p.getTotalWins(), p.getTotalLosses(), p.getTotalQuizAnswered(),
                p.getTotalQuizCorrect(), p.getTotalGrassCut(), p.getTotalRoundsPlayed(),
                winRate, quizAcc);
    }

    // ===================== HELPERS =====================

    private Room getRoom(String roomCode) {
        return roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan: " + roomCode));
    }

    public GameSession getSession(String roomCode) {
        return sessions.get(roomCode);
    }
}

    
