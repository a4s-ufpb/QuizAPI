package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Room;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.RoomRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.RoomException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.RoomNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
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

    public RoomResponse createRoom(RoomRequest roomRequest) {
        User creator = userRepository.findById(roomRequest.creatorId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        Room room = new Room();
        room.setCreator(creator);
        room.addPlayer(creator);
        roomRepository.save(room);

        RoomResponse response = room.entityToResponse();
        messagingTemplate.convertAndSend("/topic/room/" + room.getRoomId(), response);

        return response;
    }

    public void deleteRoom(UUID roomId, UUID creatorId) throws UserNotHavePermissionException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (creator.userNotHavePermission(room.getCreator())) {
            throw new UserNotHavePermissionException("Você não tem permissão para fechar essa sala!");
        }

        roomRepository.delete(room);
    }

    public RoomResponse joinRoom(UUID roomId, UUID playerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (room.containsPlayer(player)) {
            throw new RoomException("O jogador já está na sala!");
        }

        if (room.isStarted()) {
            throw new RoomException("A sala já foi iniciada!");
        }

        room.addPlayer(player);
        roomRepository.save(room);

        RoomResponse response = room.entityToResponse();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

        return response;
    }

    public void quitRoom(UUID roomId, UUID playerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (!room.containsPlayer(player)) {
            throw new RoomException("O jogador não está na sala!");
        }

        room.removePlayer(player);
        roomRepository.save(room);

        RoomResponse response = room.entityToResponse();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
    }

    public RoomResponse selectQuiz(UUID roomId, Long quizId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        room.setSelectedQuizId(quizId);
        roomRepository.save(room);

        RoomResponse response = room.entityToResponse();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

        return response;
    }

    public RoomResponse startQuiz(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        room.setStarted(true);
        roomRepository.save(room);

        RoomResponse response = room.entityToResponse();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);

        return response;
    }

    public RoomResponse findRoomById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        return room.entityToResponse();
    }
}