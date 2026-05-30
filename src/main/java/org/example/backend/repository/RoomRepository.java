package org.example.backend.repository;

import org.example.backend.entity.Room;
import org.example.backend.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByExpiresAtBeforeAndStatusNot(LocalDateTime expiresAt, RoomStatus status);

    /** Cari semua room berdasarkan status */
    List<Room> findByStatus(RoomStatus status);

    /** Cari room FINISHED yang finishedAt-nya sudah lebih dari cutoff (untuk dihapus) */
    List<Room> findByStatusAndFinishedAtBefore(RoomStatus status, LocalDateTime cutoff);
    /**
     * Fetch room WITH players eagerly in one query.
     * Prevents LazyInitializationException when used outside a transaction.
     */
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.players WHERE r.code = :code")
    Optional<Room> findByCodeWithPlayers(@Param("code") String code);

    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);
    void deleteByStatus(RoomStatus status);
}
