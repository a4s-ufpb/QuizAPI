package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.MaterialResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.MaterialType;
import jakarta.persistence.*;

import java.io.Serializable;

/** Material de apoio de um tema (vídeo, arquivo ou site). */
@Entity(name = "tb_material")
@Table(indexes = {
        @Index(name = "idx_material_theme_id", columnList = "theme_id"),
})
public class Material implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(length = 1000)
    private String link;
    @Enumerated(EnumType.STRING)
    private MaterialType type;
    @ManyToOne
    @JoinColumn(name = "theme_id")
    private Theme theme;

    public Material() {
    }

    public Material(String name, String link, MaterialType type, Theme theme) {
        this.name = name;
        this.link = link;
        this.type = type;
        this.theme = theme;
    }

    public MaterialResponse entityToResponse() {
        return new MaterialResponse(id, name, link, type);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public MaterialType getType() {
        return type;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }
}
