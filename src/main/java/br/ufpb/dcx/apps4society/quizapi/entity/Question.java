package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionMinResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "tb_question")
public class Question implements Serializable {
    public static final int MAXIMUM_NUMBER_OF_ALTERNATIVES = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String imageUrl;
    @Column(columnDefinition = "TEXT")
    private String imageBase64One;
    @Column(columnDefinition = "TEXT")
    private String imageBase64Two;
    private String imagesOrder;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Theme theme;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private User creator;
    @OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    private List<Alternative> alternatives = new ArrayList<>(MAXIMUM_NUMBER_OF_ALTERNATIVES);
    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Response> responses = new ArrayList<>();

    public Question(){

    }

    public Question(QuestionRequest questionRequest, Theme theme, User creator) {
        this.title = questionRequest.title();
        this.imageUrl = questionRequest.imageUrl();
        this.imageBase64One = questionRequest.imageBase64One();
        this.imageBase64Two = questionRequest.imageBase64Two();
        this.imagesOrder = questionRequest.imagesOrder();
        this.theme = theme;
        this.creator = creator;
    }

    public Question(Long id, String title, String imageUrl, Theme theme, User creator) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.theme = theme;
        this.creator = creator;
    }

    public QuestionResponse entityToResponse(){
        return new QuestionResponse(id,title,imageUrl,imageBase64One,imageBase64Two,imagesOrder, creator.getUuid(),theme.entityToResponse(),
                alternatives.stream().map(Alternative::entityToResponse).toList());
    }

    public QuestionMinResponse entityToMinResponse(){
        return new QuestionMinResponse(id, title, imageUrl, imageBase64One, imageBase64Two, imagesOrder, theme.entityToResponse());
    }

    public void addAlternative(Alternative alternative) {
        this.alternatives.add(alternative);
    }

    public boolean isFullListOfAlternatives(){
        return this.alternatives.size() == MAXIMUM_NUMBER_OF_ALTERNATIVES;
    }

    public void removeQuestionOfThemeList(Long id){
        theme.getQuestions().removeIf(question -> question.getId().equals(id));
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageBase64One() {
        return imageBase64One;
    }

    public void setImageBase64One(String imageBase64One) {
        this.imageBase64One = imageBase64One;
    }

    public String getImageBase64Two() {
        return imageBase64Two;
    }

    public void setImageBase64Two(String imageBase64Two) {
        this.imageBase64Two = imageBase64Two;
    }

    public String getImagesOrder() {
        return imagesOrder;
    }

    public void setImagesOrder(String imagesOrder) {
        this.imagesOrder = imagesOrder;
    }

    public List<Alternative> getAlternatives() {
        return alternatives;
    }

    public Theme getTheme() {
        return theme;
    }

    public User getCreator() {
        return creator;
    }
}
