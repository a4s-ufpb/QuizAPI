package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.util.UUID;

@Entity(name = "tb_statistic")
public class StatisticPerConclusion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID creatorId;
    private String studentName;
    private String themeName;
    private Double percentagemOfHits;
    private LocalDate date = LocalDate.now();

    public StatisticPerConclusion() {
    }

    public StatisticPerConclusion(UUID creatorId, String studentName, String themeName, Double percentagemOfHits) {
        this.creatorId = creatorId;
        this.studentName = studentName;
        this.themeName = themeName;
        this.percentagemOfHits = percentagemOfHits;
    }

    public StatisticPerConclusion(StatisticRequest statisticRequest) {
        this.creatorId = statisticRequest.creatorId();
        this.studentName = statisticRequest.studentName();
        this.themeName = statisticRequest.themeName();
        this.percentagemOfHits = statisticRequest.percentagemOfHits();

    }

    public StatisticResponse entityToResponse() {
        return new StatisticResponse(id, creatorId, studentName, themeName, percentagemOfHits, date);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public Double getPercentagemOfHits() {
        return percentagemOfHits;
    }

    public void setPercentagemOfHits(Double percentagemOfHits) {
        this.percentagemOfHits = percentagemOfHits;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }


}
