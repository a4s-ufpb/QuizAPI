package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import br.ufpb.dcx.apps4society.quizapi.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class RoomWebSocketController {

    private final RoomService roomService;

    @Autowired
    public RoomWebSocketController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/join-room")
    public RoomResponse joinRoom(UUID roomId, UUID playerId) {
        return roomService.joinRoom(roomId, playerId);
    }

    @MessageMapping("/quit-room")
    public RoomResponse quitRoom(UUID roomId, UUID playerId) {
        roomService.quitRoom(roomId, playerId);
        return roomService.findRoomById(roomId);
    }

    @MessageMapping("/select-quiz")
    public RoomResponse selectQuiz(UUID roomId, Long quizId) {
        return roomService.selectQuiz(roomId, quizId);
    }

    @MessageMapping("/start-quiz")
    public RoomResponse startQuiz(UUID roomId) {
        return roomService.startQuiz(roomId);
    }
}
