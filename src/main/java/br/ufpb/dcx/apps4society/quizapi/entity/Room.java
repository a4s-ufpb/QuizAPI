package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.room.RoomResponse;
import jakarta.persistence.*;

import java.util.*;

@Entity(name = "tb_room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID roomId;
    @ManyToOne
    private User creator;
    private Long selectedQuizId;
    private boolean started = false;
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "tb_room_players",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> players = new ArrayList<>();

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

    public boolean containsPlayer(User user) {
        return this.players.contains(user);
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
        return new RoomResponse(roomId,
                creator.entityToResponse(),
                selectedQuizId,
                started,
                players.stream()
                        .map(User::convertUserToPlayer)
                        .toList());
    }

    public List<User> getPlayers() {
        return players;
    }

    public void setPlayers(List<User> players) {
        this.players = players;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Room room = (Room) object;
        return Objects.equals(roomId, room.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }
}
