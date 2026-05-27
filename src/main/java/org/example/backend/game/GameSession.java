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
    // Guard to prevent double-execution of handleQuizResult (race between tickQuiz and answer thread)
    private volatile boolean quizResultProcessed = false;

    private final List<BombProjectile> activeBombs = new ArrayList<>();

    private int obstacleRockCount = 0;
    private boolean rockPowerUpEnabled = true;
    // powerUpPickedThisRound tracks how many power-ups have been picked up this round.
    // Max per round = 2 * round (2n rule). Probability kept constant at 0.3.
    private int powerUpPickedThisRound = 0;
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
        quizResultProcessed = false;
        phase = GamePhase.COUNTDOWN;
        countdownValue = 3;
        powerUpPickedThisRound = 0;
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

    /**
     * Add an obstacle rock at a random position that does NOT create a dead end.
     * A dead end is any open cell that would have ≤1 open neighbor after placing the rock.
     * Tries up to 20 random candidates; if none are safe, returns false (grid is too full).
     */
    public boolean addObstacleRockSafe(Random random) {
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (!rockGrid[y][x] && !playerRockGrid[y][x]
                        && getPlayerAtCell(x, y).isEmpty()) {
                    candidates.add(new int[]{x, y});
                }
            }
        }
        if (candidates.isEmpty()) return false;

        Collections.shuffle(candidates, random);
        int tries = Math.min(candidates.size(), 30);
        for (int i = 0; i < tries; i++) {
            int[] pos = candidates.get(i);
            int cx = pos[0], cy = pos[1];
            // Tentatively place the rock
            rockGrid[cy][cx] = true;
            // Check that no adjacent open cell becomes a dead end
            if (!createsDeadEnd(cx, cy)) {
                obstacleRockCount++;
                return true;
            }
            // Undo
            rockGrid[cy][cx] = false;
        }
        // All candidates create dead ends — grid is too full
        return false;
    }

    /**
     * Returns true if placing a rock at (rx, ry) would create a dead end.
     * A dead end = any open cell adjacent to (rx,ry) that now has ≤1 open neighbor.
     */
    private boolean createsDeadEnd(int rx, int ry) {
        int[][] dirs = {{0,-1},{0,1},{-1,0},{1,0}};
        for (int[] d : dirs) {
            int nx = rx + d[0], ny = ry + d[1];
            if (nx < 0 || nx >= GRID_SIZE || ny < 0 || ny >= GRID_SIZE) continue;
            if (isCellBlocked(nx, ny)) continue;
            // Count open neighbors of (nx, ny)
            int openNeighbors = 0;
            for (int[] d2 : dirs) {
                int nnx = nx + d2[0], nny = ny + d2[1];
                if (nnx < 0 || nnx >= GRID_SIZE || nny < 0 || nny >= GRID_SIZE) continue;
                if (!isCellBlocked(nnx, nny)) openNeighbors++;
            }
            if (openNeighbors <= 1) return true; // dead end
        }
        return false;
    }    public long countAlivePlayers() {
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

    public boolean isQuizResultProcessed() { return quizResultProcessed; }
    public void setQuizResultProcessed(boolean quizResultProcessed) { this.quizResultProcessed = quizResultProcessed; }

    public List<BombProjectile> getActiveBombs() { return activeBombs; }

    public int getObstacleRockCount() { return obstacleRockCount; }
    public void setObstacleRockCount(int obstacleRockCount) { this.obstacleRockCount = obstacleRockCount; }

    public boolean isRockPowerUpEnabled() { return rockPowerUpEnabled; }
    public void setRockPowerUpEnabled(boolean rockPowerUpEnabled) { this.rockPowerUpEnabled = rockPowerUpEnabled; }

    public int getPowerUpPickedThisRound() { return powerUpPickedThisRound; }
    public void incrementPowerUpPicked() { this.powerUpPickedThisRound++; }

    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public long getLastCountdownTick() { return lastCountdownTick; }
    public void setLastCountdownTick(long lastCountdownTick) { this.lastCountdownTick = lastCountdownTick; }
}
