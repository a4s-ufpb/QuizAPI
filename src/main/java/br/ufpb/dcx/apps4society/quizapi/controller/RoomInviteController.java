package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.invite.RoomInviteRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.invite.RoomInviteResponse;
import br.ufpb.dcx.apps4society.quizapi.service.RoomInviteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/room-invite")
@Tag(name = "RoomInvite", description = "Ephemeral multiplayer room invites between friends")
public class RoomInviteController {
    private final RoomInviteService roomInviteService;

    public RoomInviteController(RoomInviteService roomInviteService) {
        this.roomInviteService = roomInviteService;
    }

    @PostMapping("/{targetId}")
    public ResponseEntity<Void> sendInvite(@PathVariable UUID targetId,
                                            @RequestBody @Valid RoomInviteRequest request,
                                            @RequestHeader("Authorization") String token) {
        roomInviteService.sendInvite(targetId, request, token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<RoomInviteResponse> findMyInvite(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(roomInviteService.findMyInvite(token));
    }

    @DeleteMapping("/mine")
    public ResponseEntity<Void> dismissInvite(@RequestHeader("Authorization") String token) {
        roomInviteService.dismissInvite(token);
        return ResponseEntity.ok().build();
    }
}
