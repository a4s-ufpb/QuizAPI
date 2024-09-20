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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public RoomResponse createRoom(RoomRequest roomRequest) {
        User creator = userRepository.findById(roomRequest.creatorId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        Room room = new Room();
        room.setCreator(creator);
        room.addPlayer(creator);
        roomRepository.save(room);

        return room.entityToResponse();
    }

    public void deleteRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        roomRepository.delete(room);
    }

    public RoomResponse findRoomById(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"))
                .entityToResponse();
    }

    public RoomResponse joinRoom(UUID roomId, UUID playerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (room.containsPlayer(player)) {
            throw new RoomException("O jogador já está na sala!");
        }

        room.addPlayer(player);
        roomRepository.save(room);

        return room.entityToResponse();
    }

    public void quitRoom(UUID roomId, UUID playerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        room.removePlayer(player);
        roomRepository.save(room);
    }

    public RoomResponse selectQuiz(UUID roomId, Long quizId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        room.setSelectedQuizId(quizId);
        roomRepository.save(room);

        return room.entityToResponse();
    }

    public RoomResponse startQuiz(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Sala não encontrada"));

        room.setStarted(true);
        roomRepository.save(room);

        return room.entityToResponse();
    }
}