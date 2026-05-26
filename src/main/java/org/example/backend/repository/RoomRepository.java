package org.example.backend.repository;

import org.example.backend.entity.Room;
import org.example.backend.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);
    void deleteByStatus(RoomStatus status);
}
