package org.example.backend.controller;

import org.example.backend.dto.*;
import org.example.backend.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/{roomCode}/start")
    public ResponseEntity<Void> startGame(@PathVariable String roomCode) {
        gameService.startGame(roomCode);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomCode}/retry")
    public ResponseEntity<Void> retryGame(@PathVariable String roomCode) {
        gameService.retryGame(roomCode);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/{playerId}")
    public ResponseEntity<StatsDto> getStats(@PathVariable Long playerId) {
        return ResponseEntity.ok(gameService.getStats(playerId));
    }

    @MessageMapping("/game/{roomCode}/input")
    public void handleInput(@DestinationVariable String roomCode,
                            @Payload PlayerInputDto input) {
        gameService.handlePlayerInput(roomCode, input);
    }

    @MessageMapping("/game/{roomCode}/quiz-answer")
    public void handleQuizAnswer(@DestinationVariable String roomCode,
                                  @Payload QuizAnswerDto answer) {
        gameService.handleQuizAnswer(roomCode, answer);
    }
}
