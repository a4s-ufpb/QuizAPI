package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionMinResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionQuizResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionResponse;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "tb_question")
@Table(indexes = {
        @Index(name = "idx_question_theme_id", columnList = "theme_id"),
        @Index(name = "idx_question_creator_uuid", columnList = "creator_uuid"),
})
public class Question implements Serializable {
    public static final int MAXIMUM_NUMBER_OF_ALTERNATIVES = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String imageUrl;
    private String imageOneUrl;
    private String imageTwoUrl;
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

    public Question(QuestionRequest questionRequest, Theme theme, User creator, String imageOneUrl, String imageTwoUrl) {
        this.title = questionRequest.title();
        this.imageUrl = questionRequest.imageUrl();
        this.imageOneUrl = imageOneUrl;
        this.imageTwoUrl = imageTwoUrl;
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
        return new QuestionResponse(id,title,imageUrl,imageOneUrl,imageTwoUrl,imagesOrder, creator.getUuid(),theme.entityToResponse(),
                alternatives.stream().map(Alternative::entityToResponse).toList());
    }

    public QuestionMinResponse entityToMinResponse(){
        return new QuestionMinResponse(id, title, imageUrl, imageOneUrl, imageTwoUrl, imagesOrder, theme.entityToResponse());
    }

    public QuestionQuizResponse entityToQuizResponse(){
        return new QuestionQuizResponse(id, title, imageUrl, imageOneUrl, imageTwoUrl, imagesOrder,
                alternatives.stream().map(Alternative::entityToResponse).toList());
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

    public String getImageOneUrl() {
        return imageOneUrl;
    }

    public void setImageOneUrl(String imageOneUrl) {
        this.imageOneUrl = imageOneUrl;
    }

    public String getImageTwoUrl() {
        return imageTwoUrl;
    }

    public void setImageTwoUrl(String imageTwoUrl) {
        this.imageTwoUrl = imageTwoUrl;
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
