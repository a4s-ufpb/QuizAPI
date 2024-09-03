package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Room;
import br.ufpb.dcx.apps4society.quizapi.service.RoomService;
import org.springframework.http.HttpStatus;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(roomRequest));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.joinRoom(roomId));
    }

    @PostMapping("/{roomId}/select-quiz")
    public ResponseEntity<RoomResponse> selectQuiz(@PathVariable UUID roomId, @RequestBody Long quizId) {
        return ResponseEntity.ok(roomService.selectQuiz(roomId, quizId));
    }

    @PostMapping("/{roomId}/start-quiz")
    public ResponseEntity<RoomResponse> startQuiz(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.startQuiz(roomId));
    }
}