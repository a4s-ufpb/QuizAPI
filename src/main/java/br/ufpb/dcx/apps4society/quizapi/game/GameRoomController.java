package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateRoomRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.RoomStateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * REST público do modo multiplayer. Criação e consulta de sala não exigem login.
 * O restante do fluxo acontece via STOMP ({@link GameWebSocketController}).
 */
@RestController
@RequestMapping("/v1/game")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    public GameRoomController(GameRoomService gameRoomService) {
        this.gameRoomService = gameRoomService;
    }

    @PostMapping("/room")
    public ResponseEntity<RoomStateResponse> createRoom(@RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(201).body(gameRoomService.createRoom(request));
    }

    @GetMapping("/room/{code}")
    public ResponseEntity<RoomStateResponse> getRoom(@PathVariable String code) {
        try {
            return ResponseEntity.ok(gameRoomService.getState(code));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
