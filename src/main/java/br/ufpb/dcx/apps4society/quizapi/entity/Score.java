package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.text.DecimalFormat;

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
    @Transient
    private final Double HIT_VALUE = 97.45;
    @Transient
    private final Double REDUCE_VALUE = 1.26;

    public Score(){}

    public Score(Integer numberOfHits, Integer totalTime, User user, Theme theme) {
        this.finalResult = calculateResult(numberOfHits, totalTime);
        this.user = user;
        this.theme = theme;
    }

    private Double calculateResult(Integer numberOfHits, Integer totalTime){
        double result = (numberOfHits * HIT_VALUE) - (totalTime * REDUCE_VALUE);

        String formattedResult = new DecimalFormat("#.###").format(result).replace(",",".");

        if (result < 0) return  0.0;
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

}
