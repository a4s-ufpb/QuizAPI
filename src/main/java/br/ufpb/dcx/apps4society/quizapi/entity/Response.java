package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseDTO;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Entity(name = "tb_response")
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

    public Response() {

    }

    public Response(User user, Question question, Alternative alternative) {
        this.user = user;
        this.question = question;
        this.alternative = alternative;
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
                questionAlternatives);
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
}
