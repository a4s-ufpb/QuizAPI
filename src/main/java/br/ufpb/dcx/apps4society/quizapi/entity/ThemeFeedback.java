package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.ThemeFeedbackResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(name = "tb_theme_feedback")
@Table(indexes = {
        @Index(name = "idx_theme_feedback_theme", columnList = "theme_id"),
        @Index(name = "idx_theme_feedback_user", columnList = "user_uuid")
})
public class ThemeFeedback implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_uuid")
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ThemeFeedback() {}
    public ThemeFeedback(Theme theme, User user, String text) {
        this.theme = theme;
        this.user = user;
        this.text = text;
    }

    public ThemeFeedbackResponse entityToResponse() {
        return new ThemeFeedbackResponse(id, theme.getId(), user.getUuid(), user.getName(), text, createdAt);
    }

    public Long getId() { return id; }
    public Theme getTheme() { return theme; }
    public User getUser() { return user; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
