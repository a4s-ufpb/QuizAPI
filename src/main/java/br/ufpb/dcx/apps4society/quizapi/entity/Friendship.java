package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.friendship.FriendshipResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.FriendshipStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "tb_friendship")
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User requester;
    @ManyToOne
    private User addressee;
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Friendship() {}

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
        this.status = FriendshipStatus.PENDING;
    }

    public boolean involves(User user) {
        return requester.equals(user) || addressee.equals(user);
    }

    public User other(User user) {
        return requester.getUuid().equals(user.getUuid()) ? addressee : requester;
    }

    public FriendshipResponse entityToResponse() {
        return new FriendshipResponse(
                id,
                requester.entityToResponse(),
                addressee.entityToResponse(),
                status.name()
        );
    }

    public Long getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public User getAddressee() {
        return addressee;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
    }
}
