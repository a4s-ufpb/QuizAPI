package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import br.ufpb.dcx.apps4society.quizapi.service.RoomService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
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

    @DeleteMapping("/{roomId}/{creatorId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId, @PathVariable UUID creatorId) throws UserNotHavePermissionException {
        roomService.deleteRoom(roomId, creatorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> findRoomById(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.findRoomById(roomId));
    }
}