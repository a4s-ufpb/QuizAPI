package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.matchhistory.MatchHistoryResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.MatchMode;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "tb_match_history")
public class MatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    @Enumerated(EnumType.STRING)
    private MatchMode mode;
    private String themeName;
    private int score;
    private int total;
    private LocalDateTime playedAt = LocalDateTime.now();

    public MatchHistory() {}

    public MatchHistory(User user, MatchMode mode, String themeName, int score, int total) {
        this.user = user;
        this.mode = mode;
        this.themeName = themeName;
        this.score = score;
        this.total = total;
    }

    public MatchHistoryResponse entityToResponse() {
        return new MatchHistoryResponse(id, mode.name(), themeName, score, total, playedAt);
    }

    public User getUser() {
        return user;
    }

    public int getScore() {
        return score;
    }

    public int getTotal() {
        return total;
    }
}
