package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "tb_statistic")
public class StatisticPerConclusion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID creatorId;
    private UUID studentUuid;
    private String studentName;
    private String themeName;
    private Double percentagemOfHits;
    private LocalDateTime date = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false)
    private GameMode gameMode = GameMode.SINGLE_PLAYER;

    public StatisticPerConclusion() {
    }

    public StatisticPerConclusion(String studentName, String themeName, Double percentagemOfHits) {
        this.studentName = studentName;
        this.themeName = themeName;
        this.percentagemOfHits = percentagemOfHits;
    }

    public StatisticPerConclusion(StatisticRequest statisticRequest) {
        this.studentName = statisticRequest.studentName();
        this.themeName = statisticRequest.themeName();
        this.percentagemOfHits = statisticRequest.percentagemOfHits();
        if (statisticRequest.gameMode() != null) this.gameMode = statisticRequest.gameMode();
    }

    public StatisticResponse entityToResponse() {
        return new StatisticResponse(id, creatorId, studentUuid, studentName, themeName, percentagemOfHits, date, gameMode);
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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    public UUID getStudentUuid() { return studentUuid; }
    public void setStudentUuid(UUID studentUuid) { this.studentUuid = studentUuid; }

    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }
}
