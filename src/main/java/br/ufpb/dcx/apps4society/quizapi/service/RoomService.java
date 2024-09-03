package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.message.QuizMessage;
import br.ufpb.dcx.apps4society.quizapi.entity.Room;
import br.ufpb.dcx.apps4society.quizapi.repository.RoomRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RoomService(RoomRepository roomRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Room createRoom(UUID creatorId) {
        Room room = new Room();
        room.setRoomId(UUID.randomUUID());
        room.setCreator(userRepository.findById(creatorId).orElseThrow());
        room.setStarted(false);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("ROOM_CREATED", "Sala criada", "Server"));

        return room;
    }

    public Room joinRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("JOINED_ROOM", "Um jogador entrou na sala", "Server"));

        return room;
    }

    public Room selectQuiz(UUID roomId, Long quizId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        room.setSelectedQuizId(quizId);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("QUIZ_SELECTED", "Um quiz foi selecionado", "Server"));

        return room;
    }

    public Room startQuiz(UUID roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        room.setStarted(true);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("START_QUIZ", "O quiz começou!", "Server"));

        return room;
    }
}