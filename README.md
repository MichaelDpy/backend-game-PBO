# 🌱 LawnMower Quiz Game - Backend

## 📖 Deskripsi Proyek

**LawnMower Quiz Game** adalah aplikasi game multiplayer berbasis web yang menggabungkan elemen kompetisi memotong rumput dengan sistem quiz interaktif. Backend ini dibangun menggunakan Spring Boot dan menyediakan REST API serta WebSocket untuk komunikasi real-time antar pemain.

Pemain berlomba memotong rumput di grid 10x10 sambil menjawab pertanyaan quiz untuk bertahan hidup. Game ini mendukung 2-4 pemain dalam satu room dengan fitur power-ups, sistem nyawa, dan leaderboard.

---

## 👥 Tim Pengembang

| Nama | NIM | GitHub | Role |
|------|-----|--------|------|
| **Michael Alexius Depari** | 241401119 | [@MichaelDpy (@agnesrefayas-sketch)](https://github.com/MichaelDpy) | **Ketua Tim** |
| Achmad Caesar Ramadhan | 241401011 | [@Ahmadmapple](https://github.com/Ahmadmapple) | Developer |
| Wein Ilham Lutfi | 241401101 | [@wein359](https://github.com/wein359) | Developer |
| Naufal Khayril Lubis | 241401089 | [@Fuad](https://github.com/Fuad) | Developer |

**Mata Kuliah:** Pemrograman Berorientasi Objek (PBO)  
**Semester:** 4  
**Tahun Akademik:** 2025/2026

---

## 🛠️ Teknologi yang Digunakan

### Core Framework
- **Spring Boot 3.4.5** - Framework utama untuk membangun aplikasi Java
  - Menyediakan dependency injection, auto-configuration, dan embedded server
  - Memudahkan development dengan convention over configuration

### Database
- **H2 Database** - In-memory/file-based database
  - Mode file-based untuk persistensi data
  - H2 Console untuk monitoring database
  - Cocok untuk development dan deployment sederhana

### Security & Authentication
- **Spring Security** - Framework keamanan aplikasi
  - Mengatur autentikasi dan autorisasi
  - Integrasi dengan JWT untuk stateless authentication
- **JWT (JSON Web Token)** - Token-based authentication
  - Secure authentication tanpa session
  - Token expiration untuk keamanan

### Real-time Communication
- **Spring WebSocket** - Komunikasi real-time bidirectional
  - Mendukung STOMP protocol
  - Pub/Sub messaging untuk game state updates
- **SockJS** - WebSocket fallback
  - Kompatibilitas dengan browser yang tidak support WebSocket

### Data Validation
- **Jakarta Validation (Bean Validation)** - Validasi input
  - Annotations untuk validasi (@NotBlank, @Size, @Pattern)
  - Automatic validation di controller layer

### ORM & Database Access
- **Spring Data JPA** - Object-Relational Mapping
  - Abstraksi database operations
  - Repository pattern untuk data access
- **Hibernate** - JPA implementation
  - Automatic schema generation
  - Query optimization

### Build Tool
- **Maven** - Dependency management dan build automation
  - Centralized dependency management
  - Multi-module project support

### Deployment
- **Docker** - Containerization
  - Consistent environment across development dan production
  - Easy deployment ke cloud platforms
- **Railway** - Cloud deployment platform
  - Auto-deploy dari GitHub
  - Built-in PostgreSQL support

---

## 📁 Struktur Proyek

```
backend/
├── src/main/java/org/example/backend/
│   ├── config/                    # Konfigurasi aplikasi
│   │   ├── GlobalExceptionHandler.java    # Error handling global
│   │   ├── JwtAuthFilter.java             # Filter untuk JWT authentication
│   │   ├── JwtUtil.java                   # Utility untuk generate/validate JWT
│   │   ├── SchedulingConfig.java          # Konfigurasi scheduled tasks
│   │   ├── SecurityConfiguration.java     # Konfigurasi Spring Security
│   │   └── WebSocketConfig.java           # Konfigurasi WebSocket & STOMP
│   │
│   ├── controller/                # REST API Controllers
│   │   ├── AuthController.java            # Endpoint autentikasi (login/register)
│   │   ├── GameController.java            # Endpoint game operations
│   │   └── RoomController.java            # Endpoint room management
│   │
│   ├── dto/                       # Data Transfer Objects
│   │   ├── AuthResponse.java              # Response login/register
│   │   ├── BombDto.java                   # Data bom projectile
│   │   ├── CreateRoomRequest.java         # Request create room
│   │   ├── GameStateDto.java              # State game untuk client
│   │   ├── JoinRoomRequest.java           # Request join room
│   │   ├── LoginRequest.java              # Request login
│   │   ├── PlayerDto.java                 # Data pemain
│   │   ├── PlayerInputDto.java            # Input pemain (movement/power-up)
│   │   ├── PowerUpEventDto.java           # Event power-up collected
│   │   ├── QuizAnswerDto.java             # Jawaban quiz dari pemain
│   │   ├── QuizStateDto.java              # State quiz untuk client
│   │   ├── RegisterRequest.java           # Request register
│   │   ├── RoomDto.java                   # Data room
│   │   ├── StatsDto.java                  # Statistik pemain
│   │   └── UpdateColorRequest.java        # Request update warna
│   │
│   ├── entity/                    # JPA Entities (Database Models)
│   │   ├── BaseEntity.java                # Base class dengan ID dan timestamps
│   │   ├── Player.java                    # Entity pemain dalam room
│   │   ├── Room.java                      # Entity room game
│   │   └── UserAccount.java               # Entity akun user
│   │
│   ├── enums/                     # Enumerations
│   │   ├── GamePhase.java                 # Phase game (COUNTDOWN, PLAYING, QUIZ, GAME_OVER)
│   │   ├── MowerColor.java                # Warna mesin pemotong rumput
│   │   ├── PowerUpType.java               # Tipe power-up (ROCK, BOMB, SPEED_BOOST)
│   │   └── RoomStatus.java                # Status room (WAITING, PLAYING, FINISHED)
│   │
│   ├── game/                      # Game Logic
│   │   ├── BombProjectile.java            # Logic projectile bom
│   │   ├── GameSession.java               # Session game per room
│   │   └── PlayerGameState.java           # State pemain dalam game
│   │
│   ├── powerup/                   # Power-up System
│   │   ├── PowerUp.java                   # Abstract class power-up
│   │   ├── PowerUpFactory.java            # Factory pattern untuk create power-up
│   │   ├── BombPowerUp.java               # Implementasi power-up bom
│   │   ├── RockPowerUp.java               # Implementasi power-up batu
│   │   └── SpeedBoostPowerUp.java         # Implementasi power-up speed boost
│   │
│   ├── quiz/                      # Quiz System
│   │   ├── QuizQuestion.java              # Interface pertanyaan quiz
│   │   ├── MultipleChoiceQuestion.java    # Implementasi multiple choice
│   │   └── QuizBank.java                  # Bank soal quiz
│   │
│   ├── repository/                # Data Access Layer
│   │   ├── PlayerRepository.java          # Repository untuk Player entity
│   │   ├── RoomRepository.java            # Repository untuk Room entity
│   │   └── UserAccountRepository.java     # Repository untuk UserAccount entity
│   │
│   ├── service/                   # Business Logic Layer
│   │   ├── AuthService.java               # Service autentikasi
│   │   ├── GameService.java               # Service game logic
│   │   ├── RoomService.java               # Service room management
│   │   └── UserDetailsServiceImpl.java    # Implementation Spring Security UserDetailsService
│   │
│   └── BackendApplication.java    # Main application class
│
├── src/main/resources/
│   └── application.properties     # Konfigurasi aplikasi
│
├── Dockerfile                     # Docker configuration
├── railway.toml                   # Railway deployment config
├── pom.xml                        # Maven dependencies
└── README.md                      # Dokumentasi ini
```

---

## 🎯 Implementasi 4 Pilar OOP

### 1. **Encapsulation (Enkapsulasi)**

**Definisi:** Menyembunyikan detail implementasi dan hanya mengekspos interface yang diperlukan.

**Implementasi:**

#### a. Entity Classes dengan Private Fields
```java
// UserAccount.java
@Entity
public class UserAccount extends BaseEntity {
    @NotBlank
    @Size(min = 3, max = 20)
    @Column(nullable = false, unique = true)
    private String username;  // Private field
    
    @NotBlank
    @Column(nullable = false)
    private String passwordHash;  // Private field
    
    // Public getter/setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
```

#### b. Service Layer Encapsulation
```java
// GameService.java
@Service
public class GameService {
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    
    // Private helper methods
    private void updatePlayerPosition(PlayerGameState player, String direction) {
        // Implementation hidden from outside
    }
    
    // Public interface
    public void processPlayerInput(String roomCode, PlayerInputDto input) {
        // Uses private methods internally
    }
}
```

**Keuntungan:**
- Data integrity terjaga
- Perubahan internal tidak mempengaruhi client code
- Validasi terpusat di setter methods

---

### 2. **Inheritance (Pewarisan)**

**Definisi:** Mekanisme dimana class baru mewarisi properties dan methods dari class yang sudah ada.

**Implementasi:**

#### a. BaseEntity - Parent Class untuk Semua Entity
```java
// BaseEntity.java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters & Setters
}

// Child classes
@Entity
public class UserAccount extends BaseEntity {
    // Inherits id, createdAt, updatedAt
    private String username;
    private String passwordHash;
}

@Entity
public class Room extends BaseEntity {
    // Inherits id, createdAt, updatedAt
    private String code;
    private RoomStatus status;
}
```

#### b. PowerUp Hierarchy
```java
// PowerUp.java - Abstract parent class
public abstract class PowerUp {
    protected PowerUpType type;
    protected int posX;
    protected int posY;
    
    public abstract void activate(PlayerGameState player, GameSession session);
}

// Child classes
public class BombPowerUp extends PowerUp {
    @Override
    public void activate(PlayerGameState player, GameSession session) {
        // Specific bomb implementation
    }
}

public class RockPowerUp extends PowerUp {
    @Override
    public void activate(PlayerGameState player, GameSession session) {
        // Specific rock implementation
    }
}

public class SpeedBoostPowerUp extends PowerUp {
    @Override
    public void activate(PlayerGameState player, GameSession session) {
        // Specific speed boost implementation
    }
}
```

**Keuntungan:**
- Code reusability
- Consistent structure across entities
- Easy to add new entity types

---

### 3. **Polymorphism (Polimorfisme)**

**Definisi:** Kemampuan object untuk mengambil banyak bentuk, memungkinkan method yang sama berperilaku berbeda.

**Implementasi:**

#### a. PowerUp Polymorphism
```java
// PowerUpFactory.java
public class PowerUpFactory {
    public static PowerUp createPowerUp(PowerUpType type, int x, int y) {
        return switch (type) {
            case ROCK -> new RockPowerUp(x, y);
            case BOMB -> new BombPowerUp(x, y);
            case SPEED_BOOST -> new SpeedBoostPowerUp(x, y);
        };
    }
}

// Usage in GameService
public void activatePowerUp(PlayerGameState player) {
    PowerUp powerUp = player.getHeldPowerUp();  // Polymorphic reference
    if (powerUp != null) {
        powerUp.activate(player, session);  // Different behavior based on actual type
    }
}
```

#### b. Quiz Question Polymorphism
```java
// QuizQuestion.java - Interface
public interface QuizQuestion {
    String getQuestion();
    List<String> getChoices();
    boolean checkAnswer(int selectedIndex);
}

// MultipleChoiceQuestion.java - Implementation
public class MultipleChoiceQuestion implements QuizQuestion {
    private String question;
    private List<String> choices;
    private int correctIndex;
    
    @Override
    public String getQuestion() { return question; }
    
    @Override
    public List<String> getChoices() { return choices; }
    
    @Override
    public boolean checkAnswer(int selectedIndex) {
        return selectedIndex == correctIndex;
    }
}

// Usage
QuizQuestion question = quizBank.getRandomQuestion();  // Polymorphic
boolean correct = question.checkAnswer(playerAnswer);  // Works for any implementation
```

#### c. Repository Polymorphism (Spring Data JPA)
```java
// All repositories extend JpaRepository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
}

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByCode(String code);
}

// Usage - same interface, different implementations
@Service
public class RoomService {
    private final RoomRepository roomRepository;
    
    public Room save(Room room) {
        return roomRepository.save(room);  // Polymorphic save method
    }
}
```

**Keuntungan:**
- Flexible code
- Easy to extend with new types
- Cleaner, more maintainable code

---

### 4. **Abstraction (Abstraksi)**

**Definisi:** Menyembunyikan kompleksitas implementasi dan hanya menampilkan fungsionalitas esensial.

**Implementasi:**

#### a. Service Layer Abstraction
```java
// AuthService.java - Abstract business logic
@Service
public class AuthService {
    // Complex implementation hidden
    public AuthResponse register(RegisterRequest request) {
        // 1. Validate input
        // 2. Check if username exists
        // 3. Hash password
        // 4. Save to database
        // 5. Generate JWT token
        // 6. Return response
        // All complexity hidden from controller
    }
    
    public AuthResponse login(LoginRequest request) {
        // Complex authentication logic hidden
    }
}

// AuthController.java - Simple interface
@RestController
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));  // Simple call
    }
}
```

#### b. Repository Abstraction
```java
// Repository interface - Abstract data access
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByCode(String code);
    List<Room> findByStatus(RoomStatus status);
    // No need to know SQL or database details
}

// Usage in service
@Service
public class RoomService {
    private final RoomRepository roomRepository;
    
    public Room findByCode(String code) {
        return roomRepository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        // Database complexity abstracted away
    }
}
```

#### c. DTO Pattern - Abstraction of Data Transfer
```java
// DTO abstracts internal entity structure
public record RoomDto(
    Long id,
    String code,
    RoomStatus status,
    List<PlayerDto> players,
    Long myPlayerId
) {
    // Hides internal Room entity structure
    // Only exposes what client needs
}

// Conversion in service
public RoomDto toDto(Room room, Long myPlayerId) {
    return new RoomDto(
        room.getId(),
        room.getCode(),
        room.getStatus(),
        room.getPlayers().stream().map(this::toPlayerDto).toList(),
        myPlayerId
    );
}
```

#### d. WebSocket Abstraction
```java
// GameService.java - Abstract WebSocket complexity
@Service
public class GameService {
    private final SimpMessagingTemplate messagingTemplate;
    
    // Simple method to broadcast game state
    private void broadcastGameState(String roomCode, GameSession session) {
        GameStateDto dto = session.toDto();
        messagingTemplate.convertAndSend("/topic/room/" + roomCode, dto);
        // WebSocket complexity hidden
    }
}
```

**Keuntungan:**
- Reduced complexity
- Easier to understand and maintain
- Changes in implementation don't affect interface

---

## 🎮 Fitur Utama

### 1. **Authentication & Authorization**
- Register dan login dengan JWT
- Password hashing dengan BCrypt
- Token-based authentication
- Protected endpoints dengan Spring Security

### 2. **Room Management**
- Create room dengan kode unik (8 karakter)
- Join room dengan kode
- Support 2-4 pemain per room
- Host dapat start game
- Auto-cleanup room yang expired

### 3. **Real-time Multiplayer Game**
- WebSocket untuk komunikasi real-time
- Synchronized game state
- Player movement (WASD/Arrow keys)
- Collision detection
- Grid-based gameplay (10x10)

### 4. **Power-up System**
- **Rock** - Letakkan batu untuk menghalangi lawan
- **Bomb** - Lempar bom untuk stun lawan
- **Speed Boost** - Tingkatkan kecepatan sementara
- Random spawn di grid
- Factory pattern untuk create power-ups

### 5. **Quiz System**
- Multiple choice questions
- Random question selection
- Timed quiz (10 detik)
- Penalty untuk jawaban salah (-1 nyawa)
- Quiz bank dengan berbagai kategori

### 6. **Game Mechanics**
- Lives system (2 nyawa per pemain)
- Grass cutting untuk poin
- Collision dengan pemain lain
- Round-based gameplay
- Leaderboard dan statistik

### 7. **Persistent Statistics**
- Total games played
- Win/loss record
- Quiz accuracy
- Total grass cut
- Rounds survived

---

## 🔌 API Endpoints

### Authentication
```
POST   /api/auth/register          - Register akun baru
POST   /api/auth/login             - Login
GET    /api/auth/me                - Get user info (protected)
PUT    /api/auth/me/color          - Update warna (protected)
GET    /api/auth/me/stats          - Get statistik (protected)
```

### Room Management
```
POST   /api/rooms                  - Create room
POST   /api/rooms/join             - Join room
GET    /api/rooms/{code}           - Get room info
DELETE /api/rooms/{code}           - Disband room
```

### Game Operations
```
POST   /api/game/{roomCode}/start  - Start game
POST   /api/game/{roomCode}/retry  - Retry game
GET    /api/game/stats/{playerId}  - Get player stats
```

### WebSocket Endpoints
```
CONNECT /ws                                    - WebSocket connection
SUB     /topic/room/{roomCode}                 - Subscribe room updates
SUB     /topic/room/{roomCode}/powerup         - Subscribe power-up events
SEND    /app/game/{roomCode}/input             - Send player input
SEND    /app/game/{roomCode}/quiz-answer       - Send quiz answer
```

---

## 🚀 Cara Menjalankan

### Prerequisites
- Java 17 atau lebih tinggi
- Maven 3.6+
- Port 8080 tersedia

### Development Mode

```bash
# Clone repository
git clone <repository-url>
cd backend

# Run dengan Maven
./mvnw spring-boot:run

# Atau dengan Maven wrapper (Windows)
mvnw.cmd spring-boot:run
```

### Production Build

```bash
# Build JAR file
./mvnw clean package

# Run JAR
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Build image
docker build -t lawnmower-backend .

# Run container
docker run -p 8080:8080 \
  -e JWT_SECRET=your-secret-key \
  lawnmower-backend
```

---

## ⚙️ Konfigurasi

### Environment Variables

```properties
# JWT Configuration
JWT_SECRET=your-super-secret-key-minimum-32-characters
JWT_EXPIRATION_MS=86400000

# Server Configuration
SERVER_PORT=8080

# Database (H2)
SPRING_DATASOURCE_URL=jdbc:h2:file:./data/lawnmowerdb
```

### application.properties

```properties
# H2 Database
spring.datasource.url=jdbc:h2:file:./data/lawnmowerdb
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=${JWT_SECRET:local-dev-secret-key}
jwt.expiration-ms=86400000
```

---

## 🧪 Testing

### H2 Console
Akses database console di: `http://localhost:8080/h2-console`

**Connection details:**
- JDBC URL: `jdbc:h2:file:./data/lawnmowerdb`
- Username: `sa`
- Password: (kosong)

### API Testing dengan cURL

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# Create Room (dengan token)
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"playerName":"Player1","color":"BLUE"}'
```

---

## 📊 Database Schema

### UserAccount
- id (PK)
- username (unique)
- passwordHash
- lastColor
- totalGamesPlayed
- totalWins, totalLosses
- totalQuizAnswered, totalQuizCorrect
- totalGrassCut, totalRoundsPlayed
- createdAt, updatedAt

### Room
- id (PK)
- code (unique, 8 chars)
- status (WAITING, PLAYING, FINISHED)
- currentRound
- createdAt, updatedAt, expiresAt

### Player
- id (PK)
- name
- color
- isHost
- room_id (FK)
- sessionId
- accountUsername
- Statistics fields
- createdAt, updatedAt

---

## 🔒 Security

### Authentication
- JWT-based stateless authentication
- BCrypt password hashing
- Token expiration (24 hours default)

### Authorization
- Protected endpoints dengan @PreAuthorize
- Role-based access control
- CORS configuration untuk frontend

### Input Validation
- Jakarta Validation annotations
- Custom validators
- Global exception handling

---

## 📈 Performance Optimization

### Caching
- In-memory game sessions
- ConcurrentHashMap untuk thread-safety

### Database
- JPA query optimization
- Lazy loading untuk relationships
- Connection pooling

### WebSocket
- Efficient message broadcasting
- Selective updates
- Connection management

---

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8080
kill -9 <PID>
```

### Database Locked
```bash
# Hapus file lock
rm data/*.lock.db
```

### JWT Secret Not Set
```bash
# Set environment variable
export JWT_SECRET=your-secret-key
```

---

## 📚 Dependencies

Lihat `pom.xml` untuk daftar lengkap dependencies.

**Main Dependencies:**
- Spring Boot Starter Web
- Spring Boot Starter WebSocket
- Spring Boot Starter Security
- Spring Boot Starter Data JPA
- H2 Database
- JJWT (JWT library)
- Jakarta Validation

---

## 🤝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

## 📄 License

Project ini dibuat untuk keperluan edukasi - Tugas UAS Pemrograman Berorientasi Objek.

---

## 📞 Contact

Untuk pertanyaan atau bantuan, hubungi tim pengembang melalui GitHub.

---

**Dibuat dengan ❤️ oleh Tim PBO Semester 4**
