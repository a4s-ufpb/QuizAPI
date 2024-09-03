package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.room.QuizMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizWebSocketController {

    @MessageMapping("/join-room")
    @SendTo("/topic/room")
    public QuizMessage joinRoom(QuizMessage message) {
        System.out.println("Received join-room message: " + message);
        return new QuizMessage("JOINED_ROOM", "A user joined the room", message.sender());
    }

    @MessageMapping("/start-quiz")
    @SendTo("/topic/room")
    public QuizMessage startQuiz(QuizMessage message) {
        System.out.println("Received start-quiz message: " + message);
        return new QuizMessage("START_QUIZ", "The quiz has started", message.sender());
    }

    @MessageMapping("/submit-answer")
    @SendTo("/topic/room")
    public QuizMessage submitAnswer(QuizMessage message) {
        System.out.println("Received submit-answer message: " + message);
        return new QuizMessage("SUBMIT_ANSWER", "An answer was submitted", message.sender());
    }
}