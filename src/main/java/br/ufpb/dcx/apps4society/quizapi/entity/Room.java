package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.room.Player;
import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "tb_room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID roomId;
    @ManyToOne
    private User creator;
    private Long selectedQuizId;
    private boolean started = false;
    @Transient
    private Set<User> players = new HashSet<>();

    public Room() {
    }

    public Room(User creator) {
        this.creator = creator;
    }

    public void addPlayer(User player) {
        this.players.add(player);
    }

    public void removePlayer(User player) {
        this.players.remove(player);
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Long getSelectedQuizId() {
        return selectedQuizId;
    }

    public void setSelectedQuizId(Long selectedQuizId) {
        this.selectedQuizId = selectedQuizId;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public RoomResponse entityToResponse() {
        return new RoomResponse(roomId, creator, selectedQuizId, started, (Set<Player>) players.stream().map(player -> player.convertUserToPlayer()));
    }

    public Set<User> getPlayers() {
        return players;
    }

    public void setPlayers(Set<User> players) {
        this.players = players;
    }
}
