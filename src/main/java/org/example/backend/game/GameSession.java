package org.example.backend.game;

import org.example.backend.enums.GamePhase;
import org.example.backend.quiz.QuizQuestion;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State game in-memory untuk satu room.
 * Menerapkan ENCAPSULATION — semua field private, akses via method.
 */
public class GameSession {

    public static final int GRID_SIZE = 10;

    private final String roomCode;
    private GamePhase phase = GamePhase.COUNTDOWN;
    private int round = 1;
    private int countdownValue = 3;

    private boolean[][] grassGrid = new boolean[GRID_SIZE][GRID_SIZE];
    private boolean[][] rockGrid = new boolean[GRID_SIZE][GRID_SIZE];
    private boolean[][] playerRockGrid = new boolean[GRID_SIZE][GRID_SIZE];

    private final Map<Long, PlayerGameState> playerStates = new ConcurrentHashMap<>();

    private QuizQuestion activeQuestion = null;
    private Long quizTargetPlayerId = null;
    private long quizStartTime = 0;
    private Boolean quizAnswered = null;
    private Integer quizSelectedIndex = null;

    private final List<BombProjectile> activeBombs = new ArrayList<>();

    private int obstacleRockCount = 0;
    private boolean rockPowerUpEnabled = true;
    private double powerUpChance = 0.15;
    private String winnerId = null;

    // Per-session countdown timer — avoids shared state bug across multiple rooms
    private long lastCountdownTick = 0;

    public GameSession(String roomCode) {
        this.roomCode = roomCode;
        initGrass();
    }

    private void initGrass() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                grassGrid[y][x] = true;
                rockGrid[y][x] = false;
                playerRockGrid[y][x] = false;
            }
        }
    }

    public void resetForNewRound() {
        initGrass();
        activeBombs.clear();
        activeQuestion = null;
        quizTargetPlayerId = null;
        quizAnswered = null;
        quizSelectedIndex = null;
        phase = GamePhase.COUNTDOWN;
        countdownValue = 3;
        playerStates.values().forEach(PlayerGameState::resetForRound);
    }

    public boolean isAllGrassCut() {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (grassGrid[y][x]) return false;
            }
        }
        return true;
    }

    public int countRemainingGrass() {
        int count = 0;
        for (boolean[] row : grassGrid) {
            for (boolean cell : row) {
                if (cell) count++;
            }
        }
        return count;
    }

    public boolean isCellBlocked(int x, int y) {
        if (x < 0 || x >= GRID_SIZE || y < 0 || y >= GRID_SIZE) return true;
        return rockGrid[y][x] || playerRockGrid[y][x];
    }

    public Optional<Long> getPlayerAtCell(int x, int y) {
        return playerStates.entrySet().stream()
                .filter(e -> e.getValue().isAlive() && !e.getValue().isCrashed()
                        && e.getValue().getPosX() == x && e.getValue().getPosY() == y)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean addObstacleRock(Random random) {
        List<int[]> available = new ArrayList<>();
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (!rockGrid[y][x] && !playerRockGrid[y][x]
                        && getPlayerAtCell(x, y).isEmpty()) {
                    available.add(new int[]{x, y});
                }
            }
        }
        if (available.isEmpty()) return false;
        int[] pos = available.get(random.nextInt(available.size()));
        rockGrid[pos[1]][pos[0]] = true;
        obstacleRockCount++;
        return true;
    }

    public long countAlivePlayers() {
        return playerStates.values().stream()
                .filter(s -> s.isAlive() && !s.isCrashed())
                .count();
    }

    public Optional<Long> getLowestScoringAlivePlayer() {
        return playerStates.entrySet().stream()
                .filter(e -> e.getValue().isAlive() && !e.getValue().isCrashed())
                .min(Comparator.comparingInt(e -> e.getValue().getGrassCutThisRound()))
                .map(Map.Entry::getKey);
    }

    // ---- Getters & Setters ----

    public String getRoomCode() { return roomCode; }

    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }

    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }

    public int getCountdownValue() { return countdownValue; }
    public void setCountdownValue(int countdownValue) { this.countdownValue = countdownValue; }

    public boolean[][] getGrassGrid() { return grassGrid; }
    public void setGrassGrid(boolean[][] grassGrid) { this.grassGrid = grassGrid; }

    public boolean[][] getRockGrid() { return rockGrid; }
    public void setRockGrid(boolean[][] rockGrid) { this.rockGrid = rockGrid; }

    public boolean[][] getPlayerRockGrid() { return playerRockGrid; }
    public void setPlayerRockGrid(boolean[][] playerRockGrid) { this.playerRockGrid = playerRockGrid; }

    public Map<Long, PlayerGameState> getPlayerStates() { return playerStates; }

    public QuizQuestion getActiveQuestion() { return activeQuestion; }
    public void setActiveQuestion(QuizQuestion activeQuestion) { this.activeQuestion = activeQuestion; }

    public Long getQuizTargetPlayerId() { return quizTargetPlayerId; }
    public void setQuizTargetPlayerId(Long quizTargetPlayerId) { this.quizTargetPlayerId = quizTargetPlayerId; }

    public long getQuizStartTime() { return quizStartTime; }
    public void setQuizStartTime(long quizStartTime) { this.quizStartTime = quizStartTime; }

    public Boolean getQuizAnswered() { return quizAnswered; }
    public void setQuizAnswered(Boolean quizAnswered) { this.quizAnswered = quizAnswered; }

    public Integer getQuizSelectedIndex() { return quizSelectedIndex; }
    public void setQuizSelectedIndex(Integer quizSelectedIndex) { this.quizSelectedIndex = quizSelectedIndex; }

    public List<BombProjectile> getActiveBombs() { return activeBombs; }

    public int getObstacleRockCount() { return obstacleRockCount; }
    public void setObstacleRockCount(int obstacleRockCount) { this.obstacleRockCount = obstacleRockCount; }

    public boolean isRockPowerUpEnabled() { return rockPowerUpEnabled; }
    public void setRockPowerUpEnabled(boolean rockPowerUpEnabled) { this.rockPowerUpEnabled = rockPowerUpEnabled; }

    public double getPowerUpChance() { return powerUpChance; }
    public void setPowerUpChance(double powerUpChance) { this.powerUpChance = powerUpChance; }

    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public long getLastCountdownTick() { return lastCountdownTick; }
    public void setLastCountdownTick(long lastCountdownTick) { this.lastCountdownTick = lastCountdownTick; }
}
