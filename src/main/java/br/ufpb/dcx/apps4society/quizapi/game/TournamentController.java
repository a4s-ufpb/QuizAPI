package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateTournamentRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.TournamentStateResponse;
import br.ufpb.dcx.apps4society.quizapi.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/** REST público do modo torneio (chaveamento eliminatório) — não exige login. */
@RestController
@RequestMapping("/v1/tournament")
public class TournamentController {
    private final TournamentService tournamentService;
    private final UserService userService;

    public TournamentController(TournamentService tournamentService, UserService userService) {
        this.tournamentService = tournamentService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody CreateTournamentRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        // Capacidade: convidado até 8 jogadores; logado (USER/ADMIN) até 16.
        Role role = userService.resolveRoleOrGuest(token);
        int cap = role != null ? 16 : 8;
        int requested = request.maxPlayers() != null ? request.maxPlayers() : 8;
        int clamped = Math.min(requested, cap);
        CreateTournamentRequest adjusted = new CreateTournamentRequest(
                request.hostId(), request.hostName(), request.name(), request.themeId(),
                request.questionCount(), request.questionTimeSeconds(), clamped, request.hostUuid());
        return ResponseEntity.status(201).body(tournamentService.createTournament(adjusted));
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getState(@PathVariable String code) {
        try {
            return ResponseEntity.ok(tournamentService.getState(code));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<?> join(@PathVariable String code, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(tournamentService.join(code, body.get("playerId"), body.get("name"), body.get("userUuid")));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<?> start(@PathVariable String code, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(tournamentService.start(code, body.get("hostId")));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Trava as chaves (exige 4/8/16 jogadores) e entra na fase de configuração. */
    @PostMapping("/{code}/configure")
    public ResponseEntity<?> configure(@PathVariable String code, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(tournamentService.configure(code, body.get("hostId")));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Reabre o lobby (volta de configuração para lobby). */
    @PostMapping("/{code}/reopen")
    public ResponseEntity<?> reopen(@PathVariable String code, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(tournamentService.reopen(code, body.get("hostId")));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Define o quiz de uma rodada do chaveamento. */
    @PostMapping("/{code}/round-theme")
    public ResponseEntity<?> setRoundTheme(@PathVariable String code, @RequestBody Map<String, Object> body) {
        try {
            int roundIndex = ((Number) body.get("roundIndex")).intValue();
            Long themeId = body.get("themeId") != null ? ((Number) body.get("themeId")).longValue() : null;
            return ResponseEntity.ok(tournamentService.setRoundTheme(code, (String) body.get("hostId"), roundIndex, themeId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | ClassCastException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    e instanceof IllegalStateException ? e.getMessage() : "Requisição inválida."));
        }
    }

    /** Organizador remove um jogador do lobby. */
    @PostMapping("/{code}/kick")
    public ResponseEntity<?> kick(@PathVariable String code, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(tournamentService.kick(code, body.get("hostId"), body.get("targetId")));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Jogador sai do torneio (ou o organizador encerra ao sair). */
    @PostMapping("/{code}/leave")
    public ResponseEntity<?> leave(@PathVariable String code, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(tournamentService.leave(code, body.get("playerId")));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
