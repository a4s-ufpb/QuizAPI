package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

@Entity(name = "tb_score")
public class Score implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "result")
    private Double finalResult;
    @Transient
    private Integer numberOfHits;
    @Transient
    private Integer totalTime;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private User user;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Theme theme;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    @Transient
    private final Double HIT_VALUE = 100.0;
    // Penalidade por segundo bem mais suave que antes (era 1.26/s, o que zerava
    // a pontuação de quem acertava pouco e demorava um pouco).
    @Transient
    private final Double REDUCE_VALUE = 0.35;
    // Cada acerto vale no mínimo isso, independentemente do tempo — garante que
    // quem acertou ao menos 1 questão nunca fique com pontuação zerada.
    @Transient
    private final Double MIN_VALUE_PER_HIT = 20.0;

    public Score(){}

    public Score(Integer numberOfHits, Integer totalTime, User user, Theme theme) {
        this.finalResult = calculateResult(numberOfHits, totalTime);
        this.user = user;
        this.theme = theme;
    }

    private Double calculateResult(Integer numberOfHits, Integer totalTime){
        double result = (numberOfHits * HIT_VALUE) - (totalTime * REDUCE_VALUE);
        // Piso: cada acerto vale ao menos MIN_VALUE_PER_HIT. Sem acertos -> 0.
        double floor = numberOfHits * MIN_VALUE_PER_HIT;
        if (result < floor) result = floor;

        String formattedResult = new DecimalFormat("#.###").format(result).replace(",",".");
        return Double.valueOf(formattedResult);
    }

    public ScoreResponse entityToResponse() {
        return new ScoreResponse(id,finalResult,user.entityToResponse(),theme.entityToResponse());
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Theme getTheme() {
        return theme;
    }

    public Double getFinalResult() {
        return finalResult;
    }

    public Integer getNumberOfHits() {
        return numberOfHits;
    }

    public Integer getTotalTime() {
        return totalTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
