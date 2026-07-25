package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.StatisticFeedbackResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(name = "tb_statistic_feedback")
@Table(indexes = {
        @Index(name = "idx_stat_feedback_statistic", columnList = "statistic_id"),
        @Index(name = "idx_stat_feedback_creator", columnList = "creator_uuid")
})
public class StatisticFeedback implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "statistic_id")
    private StatisticPerConclusion statistic;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_uuid")
    private User creator;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StatisticFeedback() {}
    public StatisticFeedback(StatisticPerConclusion statistic, User creator, String text) {
        this.statistic = statistic;
        this.creator = creator;
        this.text = text;
    }

    public StatisticFeedbackResponse entityToResponse() {
        return new StatisticFeedbackResponse(id, statistic.getId(), statistic.getThemeName(),
                statistic.getStudentName(), creator.getUuid(), creator.getName(), text, createdAt);
    }

    public Long getId() { return id; }
    public StatisticPerConclusion getStatistic() { return statistic; }
    public User getCreator() { return creator; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
