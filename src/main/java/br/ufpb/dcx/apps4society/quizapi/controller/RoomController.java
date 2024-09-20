package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Room;
import br.ufpb.dcx.apps4society.quizapi.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/room")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomRequest roomRequest) {
        return ResponseEntity.status(201).body(roomService.createRoom(roomRequest));
    }

    @PostMapping
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/{playerId}")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable UUID roomId, @PathVariable UUID playerId) {
        return ResponseEntity.ok(roomService.joinRoom(roomId, playerId));
    }

    @PatchMapping("/quit/{roomId}/{playerId}")
    public ResponseEntity<Void> quitRoom(@PathVariable UUID roomId, @PathVariable UUID playerId) {
        roomService.quitRoom(roomId, playerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/select-quiz/{roomId}/{quizId}")
    public ResponseEntity<RoomResponse> selectQuiz(@PathVariable UUID roomId, @RequestBody Long quizId) {
        return ResponseEntity.ok(roomService.selectQuiz(roomId, quizId));
    }

    @PatchMapping("/start-quiz/{roomId}")
    public ResponseEntity<RoomResponse> startQuiz(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.startQuiz(roomId));
    }
}