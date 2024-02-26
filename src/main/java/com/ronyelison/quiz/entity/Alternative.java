package com.ronyelison.quiz.entity;

import com.ronyelison.quiz.dto.alternative.AlternativeRequest;
import com.ronyelison.quiz.dto.alternative.AlternativeResponse;
import jakarta.persistence.*;

@Entity(name = "tb_alternative")
public class Alternative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private Boolean correct;
    @ManyToOne
    private Question question;

    public Alternative(){
    }

    public Alternative(AlternativeRequest alternativeRequest, Question question) {
        this.description = alternativeRequest.description();
        this.correct = alternativeRequest.correct();
        this.question = question;
    }

    public AlternativeResponse entityToResponse(){
        return new AlternativeResponse(id,description,correct);
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}
