package org.example.backend.service;

import org.example.backend.dto.*;
import org.example.backend.entity.Player;
import org.example.backend.entity.Room;
import java.util.stream.Collectors;
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
import org.example.backend.entity.UserAccount;
import org.example.backend.repository.UserAccountRepository;
import org.example.backend.repository.PlayerRepository;
import org.example.backend.repository.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
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

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final UserAccountRepository userAccountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final QuizBank quizBank;
    private final PowerUpFactory powerUpFactory;
    private final RoomService roomService;
    private final Random random = new Random();
    
    public GameService(RoomRepository roomRepository,
                       PlayerRepository playerRepository,
                       UserAccountRepository userAccountRepository,
                       SimpMessagingTemplate messagingTemplate,
                       QuizBank quizBank,
                       PowerUpFactory powerUpFactory,
                       RoomService roomService) {
        this.roomRepository = roomRepository;
        this.playerRepository = playerRepository;
        this.userAccountRepository = userAccountRepository;
        this.messagingTemplate = messagingTemplate;
        this.quizBank = quizBank;
        this.powerUpFactory = powerUpFactory;
        this.roomService = roomService;
    }

    @Scheduled(fixedRate = 30000)  // cek setiap 30 detik
    @Transactional
    public void cleanupExpiredRooms() {
        LocalDateTime now = LocalDateTime.now();

        // Ambil hanya room yang belum finished dan sudah expired
        List<Room> expiredRooms = roomRepository.findByExpiresAtBeforeAndStatusNot(now, RoomStatus.FINISHED);

        for (Room room : expiredRooms) {
            if (room.getStatus() != RoomStatus.FINISHED) {
                room.markFinished();
                roomRepository.save(room);

                // Broadcast ke frontend agar player tahu room ditutup
                roomService.broadcastRoomUpdate(room, null);

                // Hapus session game jika ada
                sessions.remove(room.getCode());

                log.info("Room {} expired after 5 minutes", room.getCode());
            }
        }
    }





    // In-memory game sessions per room
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    // ===================== START GAME =====================

    public void startGame(String roomCode) {
        Room room = getRoom(roomCode);

        // Minimum 2 players required
        if (room.getPlayers().size() < 2) {
            throw new IllegalStateException("Minimal 2 pemain untuk memulai game");
        }

        room.setStatus(RoomStatus.PLAYING);
        room.setCurrentRound(1);
        roomRepository.save(room);

        // Broadcast RoomDto with PLAYING status so all WaitingRoom clients navigate to game
        roomService.broadcastRoomUpdate(room, null);

        GameSession session = new GameSession(roomCode);
        sessions.put(roomCode, session);

        spawnPlayers(session, room.getPlayers());
        broadcastState(session, room);
        startCountdown(session, room);
    }

    private void spawnPlayers(GameSession session, List<Player> players) {
        List<int[]> spawnPoints = generateSpawnPoints(players.size());
        String[] preferredDirs = {"right", "left", "down", "up"};

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int[] pos = spawnPoints.get(i);
            int x = pos[0], y = pos[1];

            // Pick a direction that doesn't immediately face a wall or rock
            String chosenDir = chooseSafeDirection(session, x, y, preferredDirs[i % 4]);

            PlayerGameState existing = session.getPlayerStates().get(p.getId());
            if (existing != null) {
                // Preserve lives, grassCutTotal, roundsSurvived — only reset position/direction
                existing.setPosX(x);
                existing.setPosY(y);
                existing.setDirection(chosenDir);
                existing.setAlive(true);
                existing.setCrashed(false);
            } else {
                // First spawn — create new state
                PlayerGameState state = new PlayerGameState(p.getId(), x, y, chosenDir);
                session.getPlayerStates().put(p.getId(), state);
            }
        }
    }

    /**
     * Choose a direction from which the player won't immediately crash.
     * Checks 2 cells ahead so the player has at least one move before hitting anything.
     * Tries the preferred direction first, then cycles through all four.
     * Falls back to preferred if all directions are blocked.
     */
    private String chooseSafeDirection(GameSession session, int x, int y, String preferred) {
        String[] allDirs = {"right", "left", "down", "up"};
        // Try preferred first (2 cells clear)
        if (isSafeDirection(session, x, y, preferred, 2)) return preferred;
        // Try other directions with 2 cells clear
        for (String dir : allDirs) {
            if (!dir.equals(preferred) && isSafeDirection(session, x, y, dir, 2)) return dir;
        }
        // Fallback: try with just 1 cell clear
        if (isSafeDirection(session, x, y, preferred, 1)) return preferred;
        for (String dir : allDirs) {
            if (!dir.equals(preferred) && isSafeDirection(session, x, y, dir, 1)) return dir;
        }
        return preferred; // last resort
    }

    /** Returns true if the next `steps` cells in `dir` from (x,y) are all clear. */
    private boolean isSafeDirection(GameSession session, int x, int y, String dir, int steps) {
        int nx = x, ny = y;
        for (int i = 0; i < steps; i++) {
            switch (dir) {
                case "up"    -> ny--;
                case "down"  -> ny++;
                case "left"  -> nx--;
                case "right" -> nx++;
            }
            if (nx < 0 || nx >= GameSession.GRID_SIZE || ny < 0 || ny >= GameSession.GRID_SIZE) return false;
            if (session.isCellBlocked(nx, ny)) return false;
        }
        return true;
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

    @Transactional
    @Scheduled(fixedRate = 100)
    public void gameTick() {
        for (Map.Entry<String, GameSession> entry : sessions.entrySet()) {
            String roomCode = entry.getKey();
            GameSession session = entry.getValue();

            // Skip sessions that are already game over
            if (session.getPhase() == GamePhase.GAME_OVER) {
                sessions.remove(roomCode);
                continue;
            }

            try {
                Room room = roomRepository.findByCodeWithPlayers(roomCode).orElse(null);
                if (room == null || room.getStatus() == RoomStatus.FINISHED) {
                    sessions.remove(roomCode);
                    continue;
                }
                if (room.getStatus() != RoomStatus.PLAYING) continue;

                switch (session.getPhase()) {
                    case COUNTDOWN -> tickCountdown(session, room);
                    case PLAYING   -> tickPlaying(session, room);
                    case QUIZ      -> tickQuiz(session, room);
                    default        -> { }
                }
            } catch (Exception e) {
                log.error("Error in game tick for room {}: {}", roomCode, e.getMessage());
            }
        }
    }

    private void tickCountdown(GameSession session, Room room) {
        long now = System.currentTimeMillis();
        if (now - session.getLastCountdownTick() < 1000) return;
        session.setLastCountdownTick(now);

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
        if (state == null || !state.isAlive() || state.isCrashed() || state.isStunned()) return;

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
            if (!state.isAlive() || state.isCrashed() || state.isStunned()) continue;
            movePlayer(session, state);
        }

        // Proses bom yang sudah sampai
        processBombs(session);

        // Cek apakah semua rumput sudah dipotong → akhiri ronde
        if (session.isAllGrassCut()) {
            endRound(session, room);
            return;
        }

        // Cek apakah semua pemain sudah mati/crash di ronde ini → akhiri ronde
        if (session.countAlivePlayers() == 0) {
            endRound(session, room);
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
        Optional<Long> otherPlayerId = session.getPlayerAtCell(newX, newY);
        if (otherPlayerId.isPresent()) {
            PlayerGameState otherPlayer = session.getPlayerStates().get(otherPlayerId.get());
            if (otherPlayer != null) {
                // Logika collision berdasarkan arah hadap:
                // 1. Jika kedua pemain saling berhadapan (head-on collision) → salah satu mati (random)
                // 2. Jika pemain A menghadap ke pemain B, tapi B menghadap ke arah lain → A yang mati
                
                boolean currentPlayerFacingOther = isFacingPlayer(state, otherPlayer);
                boolean otherPlayerFacingCurrent = isFacingPlayer(otherPlayer, state);
                
                if (currentPlayerFacingOther && otherPlayerFacingCurrent) {
                    // Head-on collision: salah satu mati secara random
                    if (random.nextBoolean()) {
                        state.setCrashed(true);
                        state.setAlive(false);
                    } else {
                        otherPlayer.setCrashed(true);
                        otherPlayer.setAlive(false);
                    }
                } else if (currentPlayerFacingOther) {
                    // Pemain ini menghadap ke pemain lain, tapi pemain lain tidak menghadap ke sini
                    // → Pemain ini yang mati
                    state.setCrashed(true);
                    state.setAlive(false);
                } else if (otherPlayerFacingCurrent) {
                    // Pemain lain menghadap ke pemain ini, tapi pemain ini tidak menghadap ke sana
                    // → Pemain lain yang mati
                    otherPlayer.setCrashed(true);
                    otherPlayer.setAlive(false);
                } else {
                    // Tidak ada yang saling menghadap (side collision) → yang bergerak mati
                    state.setCrashed(true);
                    state.setAlive(false);
                }
            }
            return;
        }

        // Gerak berhasil
        state.setPosX(newX);
        state.setPosY(newY);

        // Potong rumput
        if (session.getGrassGrid()[newY][newX]) {
            session.getGrassGrid()[newY][newX] = false;
            state.addGrassCut();

            // Power-up: max 2*round per ronde (2n rule), probability 0.3 per grass tile
            int maxThisRound = 2 * session.getRound();
            boolean canPickup = session.getPowerUpPickedThisRound() < maxThisRound
                    && random.nextDouble() < 0.3;
            if (canPickup) {
                PowerUp pu = powerUpFactory.createRandom(session.isRockPowerUpEnabled());
                session.incrementPowerUpPicked();

                // Speed boost langsung aktif, tidak disimpan di slot
                if (pu.isAutoActivate()) {
                    state.activateSpeedBoost(pu.getEffectDurationMs());
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

    /**
     * Cek apakah player1 menghadap ke arah player2
     */
    private boolean isFacingPlayer(PlayerGameState player1, PlayerGameState player2) {
        int dx = player2.getPosX() - player1.getPosX();
        int dy = player2.getPosY() - player1.getPosY();
        String dir = player1.getDirection();
        
        // Cek apakah arah hadap player1 menuju ke posisi player2
        return switch (dir) {
            case "right" -> dx > 0 && dy == 0;
            case "left" -> dx < 0 && dy == 0;
            case "down" -> dy > 0 && dx == 0;
            case "up" -> dy < 0 && dx == 0;
            default -> false;
        };
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
                    // Stun the target for 2 seconds instead of killing
                    target.applyStun(2000);
                }
                it.remove();
            }
        }
    }

    // ===================== QUIZ =====================

    private void tickQuiz(GameSession session, Room room) {
        if (session.getQuizTargetPlayerId() == null) return;
        if (session.isQuizResultProcessed()) return;

        long elapsed = System.currentTimeMillis() - session.getQuizStartTime();
        if (elapsed >= 10000 && session.getQuizAnswered() == null) {
            // Waktu habis — tandai dulu agar tidak double-fire
            session.setQuizAnswered(false);
            handleQuizResult(session, room, false);
        }
    }

    public void handleQuizAnswer(String roomCode, QuizAnswerDto answer) {
        GameSession session = sessions.get(roomCode);
        if (session == null || session.getPhase() != GamePhase.QUIZ) return;
        if (!answer.playerId().equals(session.getQuizTargetPlayerId())) return;
        if (session.getQuizAnswered() != null) return; // sudah dijawab
        if (session.isQuizResultProcessed()) return;

        boolean correct = session.getActiveQuestion().isCorrect(answer.selectedIndex());
        session.setQuizAnswered(correct);
        session.setQuizSelectedIndex(answer.selectedIndex());

        Room room = getRoom(roomCode);

        // Update Player entity stats (kumulatif lifetime) dan catat di GameSession untuk endGame
        playerRepository.findById(answer.playerId()).ifPresent(p -> {
            p.setTotalQuizAnswered(p.getTotalQuizAnswered() + 1);
            if (correct) p.setTotalQuizCorrect(p.getTotalQuizCorrect() + 1);
            playerRepository.save(p);
        });
        // Catat quiz game ini di session agar endGame bisa kirim delta yang benar ke UserAccount
        session.incrementQuizAnswered(answer.playerId(), correct);

        broadcastState(session, room);

        // Tunggu 2 detik agar semua lihat hasilnya, lalu lanjut
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            handleQuizResult(session, room, correct);
        }).start();
    }

    private synchronized void handleQuizResult(GameSession session, Room room, boolean correct) {
        // Guard: only process once per quiz round
        if (session.isQuizResultProcessed()) return;
        session.setQuizResultProcessed(true);

        if (!correct) {
            PlayerGameState state = session.getPlayerStates().get(session.getQuizTargetPlayerId());
            if (state != null) {
                state.loseLife();
            }
        }

        // Broadcast the updated state (with new lives count) before transitioning
        broadcastState(session, room);

        // Check if game should end: ≤1 player still has lives remaining
        long playersWithLives = session.getPlayerStates().values().stream()
                .filter(s -> s.getLives() > 0)
                .count();
        if (playersWithLives <= 1) {
            endGame(session, room);
            return;
        }

        // Lanjut ke ronde berikutnya
        startNextRound(session, room);
    }

    // ===================== ROUND END =====================

    private void endRound(GameSession session, Room room) {
        // Award roundsSurvived to all players who still have lives (regardless of alive this round)
        session.getPlayerStates().values().forEach(s -> {
            if (s.getLives() > 0) s.incrementRoundsSurvived();
        });

        // Find the player with the least grass cut this round among those who still have lives.
        // This includes players who died this round (lives > 0 but crashed/not alive).
        Optional<Long> loser = session.getPlayerStates().entrySet().stream()
                .filter(e -> e.getValue().getLives() > 0)
                .min(Comparator.comparingInt(e -> e.getValue().getGrassCutThisRound()))
                .map(Map.Entry::getKey);

        if (loser.isEmpty()) {
            // No players have lives left — go to game over
            endGame(session, room);
            return;
        }

        session.setPhase(GamePhase.QUIZ);
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

        session.resetForNewRound();

        // Tambah batu penghalang setiap kelipatan 3n+1 (ronde 4, 7, 10, 13...)
        // PENTING: Panggil SETELAH resetForNewRound() agar batu tidak di-clear
        addObstacleRocksIfNeeded(session, newRound);

        // Restore alive=true for players who still have lives before respawning
        session.getPlayerStates().values().forEach(s -> {
            if (s.getLives() > 0) s.setAlive(true);
        });

        // Only respawn players who still have lives
        List<Player> activePlayers = room.getPlayers().stream()
                .filter(p -> {
                    PlayerGameState s = session.getPlayerStates().get(p.getId());
                    return s != null && s.getLives() > 0;
                }).toList();

        spawnPlayers(session, activePlayers);

        broadcastState(session, room);
        startCountdown(session, room);
    }

    private void addObstacleRocksIfNeeded(GameSession session, int round) {
        // Batu muncul di ronde 3n+1 dimana n = 1, 2, 3, ...
        // Yaitu ronde 4, 7, 10, 13, 16, ...
        // Di setiap ronde tersebut, tambahkan 2n batu
        // Ronde 4 (n=1) → 2 batu
        // Ronde 7 (n=2) → 4 batu
        // Ronde 10 (n=3) → 6 batu
        // dst.
        
        if (round < 4) return;
        if ((round - 1) % 3 != 0) return;

        int n = (round - 1) / 3; // n=1 di ronde 4, n=2 di ronde 7, dst.
        int rocksToAdd = 2 * n;

        log.debug("Round {}: Adding {} obstacle rocks (n={})", round, rocksToAdd, n);

        for (int i = 0; i < rocksToAdd; i++) {
            boolean ok = session.addObstacleRockSafe(random);
            if (!ok) {
                // Tidak ada posisi aman tersisa — matikan power-up batu untuk mencegah jalan buntu
                session.setRockPowerUpEnabled(false);
                log.info("Grid too full at round {}. Rock power-up disabled to prevent dead ends.", round);
                break;
            }
        }
    }

    // ===================== GAME OVER =====================

    private void endGame(GameSession session, Room room) {
        session.setPhase(GamePhase.GAME_OVER);

        // The winner is the last player with lives remaining
        Optional<PlayerGameState> winner = session.getPlayerStates().values().stream()
                .filter(s -> s.getLives() > 0)
                .findFirst();

        winner.ifPresent(w -> {
            w.incrementRoundsSurvived();
            session.setWinnerId(String.valueOf(w.getPlayerId()));
            playerRepository.findById(w.getPlayerId()).ifPresent(p -> {
                p.setTotalWins(p.getTotalWins() + 1);
                p.setTotalGamesPlayed(p.getTotalGamesPlayed() + 1);
                p.setTotalRoundsPlayed(p.getTotalRoundsPlayed() + w.getRoundsSurvived());
                p.setTotalGrassCut(p.getTotalGrassCut() + w.getGrassCutTotal());
                playerRepository.save(p);
                // Use per-game quiz delta (not lifetime total) to avoid double-counting
                int[] quizDelta = session.getQuizDelta(w.getPlayerId());
                updateAccountStats(p, true, w.getGrassCutTotal(), w.getRoundsSurvived(),
                        quizDelta[0], quizDelta[1]);
            });
        });

        // Update stats for all losing players (lives <= 0, i.e. not the winner)
        session.getPlayerStates().values().stream()
                .filter(s -> s.getLives() <= 0)
                .forEach(s -> playerRepository.findById(s.getPlayerId()).ifPresent(p -> {
                    p.setTotalLosses(p.getTotalLosses() + 1);
                    p.setTotalGamesPlayed(p.getTotalGamesPlayed() + 1);
                    p.setTotalRoundsPlayed(p.getTotalRoundsPlayed() + s.getRoundsSurvived());
                    p.setTotalGrassCut(p.getTotalGrassCut() + s.getGrassCutTotal());
                    playerRepository.save(p);
                    int[] quizDelta = session.getQuizDelta(s.getPlayerId());
                    updateAccountStats(p, false, s.getGrassCutTotal(), s.getRoundsSurvived(),
                            quizDelta[0], quizDelta[1]);
                }));

        room.markFinished();
        roomRepository.save(room);

        broadcastState(session, room);
        sessions.remove(room.getCode());
    }

    public void retryGame(String roomCode) {
        Room room = getRoom(roomCode);
        
        // Validasi: Minimal 2 players untuk main lagi
        if (room.getPlayers().size() < 2) {
            throw new IllegalStateException("Minimal 2 pemain untuk memulai game. Tidak bisa main lagi sendirian!");
        }
        
        room.setStatus(RoomStatus.PLAYING);
        room.setCurrentRound(1);
        roomRepository.save(room);

        // Create a fresh session — clears all PlayerGameState so lives/stats start from zero
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
                                2, 0, 0, 0, 0, "right", true, false, false, null, 0, false, 0L);
                    }
                    return new PlayerDto(p.getId(), p.getName(), p.getColor(), p.isHost(),
                            s.getLives(), s.getGrassCutTotal(), s.getGrassCutThisRound(),
                            s.getPosX(), s.getPosY(), s.getDirection(),
                            s.isAlive(), s.isCrashed(), s.isSpeedBoosted(), s.getHeldPowerUp(),
                            s.getRoundsSurvived(), s.isStunned(), s.getStunEndTime());
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

        List<BombDto> bombDtos = session.getActiveBombs().stream()
                .map(b -> new BombDto(
                        b.getThrowerPlayerId(), b.getTargetPlayerId(),
                        b.getFromX(), b.getFromY(), b.getToX(), b.getToY(),
                        b.getLaunchTime(), b.getArrivalTime()))
                .collect(Collectors.toList());

        // Merge obstacle rocks + player-placed rocks into one grid for the frontend
        boolean[][] mergedRockGrid = new boolean[GameSession.GRID_SIZE][GameSession.GRID_SIZE];
        boolean[][] obstacleRocks = session.getRockGrid();
        boolean[][] playerRocks   = session.getPlayerRockGrid();
        for (int y = 0; y < GameSession.GRID_SIZE; y++) {
            for (int x = 0; x < GameSession.GRID_SIZE; x++) {
                mergedRockGrid[y][x] = obstacleRocks[y][x] || playerRocks[y][x];
            }
        }

        // Build leaderboard: sort by roundsSurvived desc, then grassCutTotal desc
        List<PlayerDto> leaderboard = playerDtos.stream()
                .sorted(Comparator.comparingInt(PlayerDto::roundsSurvived).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerDto::grassCut).reversed()))
                .collect(Collectors.toList());

        return new GameStateDto(
                session.getRoomCode(),
                session.getPhase(),
                session.getRound(),
                session.getCountdownValue(),
                session.getGrassGrid(),
                mergedRockGrid,
                playerDtos,
                quizState,
                session.getWinnerId(),
                bombDtos,
                leaderboard
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

    /** Update stats UserAccount jika player terhubung ke akun */
    private void updateAccountStats(Player p, boolean won, int grassCut, int rounds,
                                     int quizAnswered, int quizCorrect) {
        if (p.getAccountUsername() == null) return;
        userAccountRepository.findByUsername(p.getAccountUsername()).ifPresent(acc -> {
            acc.setTotalGamesPlayed(acc.getTotalGamesPlayed() + 1);
            if (won) acc.setTotalWins(acc.getTotalWins() + 1);
            else acc.setTotalLosses(acc.getTotalLosses() + 1);
            acc.setTotalGrassCut(acc.getTotalGrassCut() + grassCut);
            acc.setTotalRoundsPlayed(acc.getTotalRoundsPlayed() + rounds);
            acc.setTotalQuizAnswered(acc.getTotalQuizAnswered() + quizAnswered);
            acc.setTotalQuizCorrect(acc.getTotalQuizCorrect() + quizCorrect);
            userAccountRepository.save(acc);
        });
    }

    private Room getRoom(String roomCode) {
        return roomRepository.findByCodeWithPlayers(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room tidak ditemukan: " + roomCode));
    }

    public GameSession getSession(String roomCode) {
        return sessions.get(roomCode);
    }
}
