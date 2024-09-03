package br.ufpb.dcx.apps4society.quizapi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

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

    public Room() {
    }

    public Room(User creator) {
        this.creator = creator;
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
}
