package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateRoomRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.GameConfig;
import br.ufpb.dcx.apps4society.quizapi.game.dto.RoomStateResponse;
import br.ufpb.dcx.apps4society.quizapi.service.UserService;
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
    private final UserService userService;

    public GameRoomController(GameRoomService gameRoomService, UserService userService) {
        this.gameRoomService = gameRoomService;
        this.userService = userService;
    }

    @PostMapping("/room")
    public ResponseEntity<RoomStateResponse> createRoom(
            @RequestBody CreateRoomRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Role role = userService.resolveRoleOrGuest(token);
        CreateRoomRequest clamped = new CreateRoomRequest(
                request.hostId(), request.hostName(), request.themeId(),
                clampConfigToRole(request.config(), role));
        return ResponseEntity.status(201).body(gameRoomService.createRoom(clamped));
    }

    // Capacidade máxima da sala e tamanho de equipe permitidos por papel:
    //  - convidado: 12 jogadores / 2 por equipe
    //  - logado:    até 24 / até 3 por equipe
    //  - admin:     até 48 / até 4 por equipe
    private GameConfig clampConfigToRole(GameConfig config, Role role) {
        if (config == null) return null;
        int roomCap = role == Role.ADMIN ? 48 : role == Role.USER ? 24 : 12;
        int teamCap = role == Role.ADMIN ? 4 : role == Role.USER ? 3 : 2;

        Integer maxPlayers = config.maxPlayers();
        int max = maxPlayers != null ? Math.min(maxPlayers, roomCap) : roomCap;

        Integer maxPerTeam = config.maxPlayersPerTeam();
        Integer perTeam = maxPerTeam != null ? Math.min(maxPerTeam, teamCap) : null;

        return new GameConfig(
                config.roomMode(), config.scoringMode(), config.advanceMode(),
                config.questionTimeSeconds(), config.questionCount(),
                perTeam, config.gameStyle(), max);
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
