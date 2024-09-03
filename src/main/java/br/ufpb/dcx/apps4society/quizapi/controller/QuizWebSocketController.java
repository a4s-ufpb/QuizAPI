package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.message.QuizMessage;
import br.ufpb.dcx.apps4society.quizapi.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class QuizWebSocketController {
    private final RoomService roomService;
    @Autowired
    public QuizWebSocketController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/join-room")
    @SendTo("/topic/room")
    public QuizMessage joinRoom(QuizMessage message) {
        // Lógica para adicionar o jogador à sala
        // Exemplo: roomService.joinRoom(roomId);
        String content = HtmlUtils.htmlEscape(message.content()) + " joined the room!";
        return new QuizMessage("JOINED_ROOM", content, message.sender());
    }

    @MessageMapping("/start-quiz")
    @SendTo("/topic/room")
    public QuizMessage startQuiz(QuizMessage message) {
        // Lógica para iniciar o quiz na sala
        // Exemplo: roomService.startQuiz(roomId);
        String content = HtmlUtils.htmlEscape(message.content()) + " started the quiz!";
        return new QuizMessage("START_QUIZ", content, message.sender());
    }

    @MessageMapping("/submit-answer")
    @SendTo("/topic/room")
    public QuizMessage submitAnswer(QuizMessage message) {
        // Lógica para processar a resposta do jogador
        // Exemplo: roomService.submitAnswer(roomId, answer);
        String content = HtmlUtils.htmlEscape(message.content()) + " submitted an answer!";
        return new QuizMessage("SUBMIT_ANSWER", content, message.sender());
    }
}
