package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseDTO;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Entity(name = "tb_response")
@Table(indexes = {
        @Index(name = "idx_response_user_uuid", columnList = "user_uuid"),
        @Index(name = "idx_response_question_id", columnList = "question_id"),
        @Index(name = "idx_response_game_mode_date_time", columnList = "game_mode, date_time"),
})
public class Response implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate dateTime = LocalDate.now();
    @ManyToOne(cascade = CascadeType.PERSIST)
    private User user;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Question question;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Alternative alternative;
    @Enumerated(EnumType.STRING)
    private GameMode gameMode = GameMode.SINGLE_PLAYER;

    public Response() {

    }

    public Response(User user, Question question, Alternative alternative) {
        this.user = user;
        this.question = question;
        this.alternative = alternative;
    }

    public Response(User user, Question question, Alternative alternative, GameMode gameMode) {
        this.user = user;
        this.question = question;
        this.alternative = alternative;
        this.gameMode = gameMode;
    }

    public ResponseDTO entityToResponse(){
        List<br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeResponse> questionAlternatives =
                question.getAlternatives().stream()
                        .map(Alternative::entityToResponse)
                        .toList();

        return new ResponseDTO(id, dateTime,
                user.entityToResponse(),
                question.entityToMinResponse(),
                alternative.entityToResponse(),
                questionAlternatives,
                gameMode);
    }

    public LocalDate getDateTime() {
        return dateTime;
    }

    public User getUser() {
        return user;
    }

    public Question getQuestion() {
        return question;
    }

    public Alternative getAlternative() {
        return alternative;
    }

    public GameMode getGameMode() {
        return gameMode;
    }
}
