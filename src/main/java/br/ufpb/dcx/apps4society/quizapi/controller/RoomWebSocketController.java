package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.room.ws.RoomAndPlayerId;
import br.ufpb.dcx.apps4society.quizapi.dto.room.ws.SelectQuizRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.room.ws.StartQuizRequest;
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
    public RoomResponse joinRoom(RoomAndPlayerId joinRoomRequest) {
        return roomService.joinRoom(joinRoomRequest.roomId(), joinRoomRequest.playerId());
    }

    @MessageMapping("/quit-room")
    public RoomResponse quitRoom(RoomAndPlayerId quitRoomRequest) {
        roomService.quitRoom(quitRoomRequest.roomId(), quitRoomRequest.playerId());
        return roomService.findRoomById(quitRoomRequest.roomId());
    }

    @MessageMapping("/select-quiz")
    public RoomResponse selectQuiz(SelectQuizRequest selectQuizRequest) {
        return roomService.selectQuiz(selectQuizRequest.roomId(), selectQuizRequest.quizId());
    }

    @MessageMapping("/start-quiz")
    public RoomResponse startQuiz(StartQuizRequest startQuizRequest) {
        return roomService.startQuiz(startQuizRequest.roomId());
    }
}
