package br.ufpb.dcx.apps4society.quizapi.controller;

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
    public ResponseEntity<Room> createRoom(@RequestBody UUID creatorId) {
        Room room = roomService.createRoom(creatorId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Room> joinRoom(@PathVariable UUID roomId) {
        Room room = roomService.joinRoom(roomId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/select-quiz")
    public ResponseEntity<Room> selectQuiz(@PathVariable UUID roomId, @RequestBody Long quizId) {
        Room room = roomService.selectQuiz(roomId, quizId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/start-quiz")
    public ResponseEntity<Room> startQuiz(@PathVariable UUID roomId) {
        Room room = roomService.startQuiz(roomId);
        return ResponseEntity.ok(room);
    }
}