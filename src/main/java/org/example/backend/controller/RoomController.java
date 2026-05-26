package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.*;
import org.example.backend.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomDto> joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        return ResponseEntity.ok(roomService.joinRoom(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomDto> getRoom(@PathVariable String code,
                                            @RequestParam(required = false) Long playerId) {
        return ResponseEntity.ok(roomService.getRoom(code, playerId));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> disbandRoom(@PathVariable String code) {
        roomService.disbandRoom(code);
        return ResponseEntity.noContent().build();
    }
}
