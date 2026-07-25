package br.ufpb.dcx.apps4society.quizapi.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "tb_theme_like")
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_theme_like_user_theme", columnNames = {"user_uuid", "theme_id"})
}, indexes = {
        @Index(name = "idx_theme_like_theme", columnList = "theme_id")
})
public class ThemeLike implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_uuid")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ThemeLike() {}
    public ThemeLike(User user, Theme theme) {
        this.user = user;
        this.theme = theme;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Theme getTheme() { return theme; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ThemeLike t)) return false;
        return Objects.equals(id, t.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
