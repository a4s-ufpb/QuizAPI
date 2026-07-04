package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(name = "tb_theme")
@Table(indexes = {
        @Index(name = "idx_theme_creator_uuid", columnList = "creator_uuid"),
})
public class Theme implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String imageUrl;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private User creator;
    // LAZY: listar temas não precisa carregar todas as questões (com base64
    // de imagem) de cada um — só é acessado explicitamente quando necessário.
    @OneToMany(mappedBy = "theme", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Question> questions = new ArrayList<>();
    @OneToMany(mappedBy = "theme", cascade = CascadeType.REMOVE)
    private List<Score> ranking = new ArrayList<>();

    public Theme(){

    }

    public Theme(String name, String imageUrl, User creator) {
        this.name = name;
        this.creator = creator;
        this.imageUrl = imageUrl;
    }

    public Theme(Long id, String name, User creator) {
        this.id = id;
        this.name = name;
        this.creator = creator;
    }

    public ThemeResponse entityToResponse(){
        return new ThemeResponse(id, name, imageUrl);
    }

    public void addQuestion(Question question){
        this.questions.add(question);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Theme theme = (Theme) object;
        return Objects.equals(id, theme.id) && Objects.equals(name, theme.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public User getCreator() {
        return creator;
    }
}
