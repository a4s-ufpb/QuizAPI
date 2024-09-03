package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.room.QuizMessage;
import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Room;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.RoomRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.RoomNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.RoomStartedException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ThemeRepository themeRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, ThemeRepository themeRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.themeRepository = themeRepository;
    }

    public RoomResponse createRoom(RoomRequest roomRequest) {
        User user = userRepository.findById(roomRequest.creatorId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado!"));

        Room room = new Room(user);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("ROOM_CREATED", "Sala criada", "Server"));

        return room.entityToResponse();
    }

    public RoomResponse joinRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada!"));

        if (room.isStarted()) {
            throw new RoomStartedException("A sala já foi iniciada!");
        }

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("JOINED_ROOM", "Um jogador entrou na sala", "Server"));

        return room.entityToResponse();
    }

    public RoomResponse selectQuiz(UUID roomId, Long themeId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada!"));

        themeRepository.findById(themeId)
                        .orElseThrow(() -> new ThemeNotFoundException("Tema não encontrado"));

        room.setSelectedQuizId(themeId);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("QUIZ_SELECTED", "Um quiz foi selecionado", "Server"));

        return room.entityToResponse();
    }

    public RoomResponse startQuiz(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada!"));

        room.setStarted(true);
        roomRepository.save(room);

        messagingTemplate.convertAndSend("/topic/room", new QuizMessage("START_QUIZ", "O quiz começou!", "Server"));

        return room.entityToResponse();
    }
}