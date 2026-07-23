package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionMinResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionQuizResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionUpdate;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ImageSizeLimitExceededException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.QuestionNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import br.ufpb.dcx.apps4society.quizapi.util.ImageValidator;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionService {
    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private QuestionRepository questionRepository;
    private ThemeRepository themeRepository;
    private UserService userService;
    private ImageStorageService imageStorageService;

    @Autowired
    public QuestionService(QuestionRepository questionRepository, ThemeRepository themeRepository,
                           UserService userService, ImageStorageService imageStorageService) {
        this.questionRepository = questionRepository;
        this.themeRepository = themeRepository;
        this.userService = userService;
        this.imageStorageService = imageStorageService;
    }

    public QuestionResponse insertQuestion(QuestionRequest questionRequest, Long idTheme, String token) throws UserNotHavePermissionException, ImageSizeLimitExceededException {
        User creator = userService.findUserByToken(token);

        Theme theme = themeRepository.findById(idTheme)
                .orElseThrow(()-> new ThemeNotFoundException("Tema não encontrado"));

        if (creator.userNotHavePermission(theme.getCreator())){
            throw new UserNotHavePermissionException("Você não tem permissão para cadastrar questões nesse Tema");
        }

        validateImages(questionRequest.imageOneUrl(), questionRequest.imageTwoUrl());

        String imageOneUrl = imageStorageService.upload(questionRequest.imageOneUrl());
        String imageTwoUrl = imageStorageService.upload(questionRequest.imageTwoUrl());

        Question question = new Question(questionRequest, theme, creator, imageOneUrl, imageTwoUrl);
        theme.addQuestion(question);
        creator.addQuestion(question);

        questionRepository.save(question);
        log.info("questão criada: '{}' no tema id={} por {}", questionRequest.title(), idTheme, creator.getEmail());
        return question.entityToResponse();
    }

    public QuestionResponse insertQuestionMultipart(String title, String imageUrl, String imageOneUrl,
                                                     String imageTwoUrl, String imagesOrder,
                                                     MultipartFile imageFile1, MultipartFile imageFile2,
                                                     Long idTheme, String token) throws UserNotHavePermissionException {
        User creator = userService.findUserByToken(token);

        Theme theme = themeRepository.findById(idTheme)
                .orElseThrow(() -> new ThemeNotFoundException("Tema não encontrado"));

        if (creator.userNotHavePermission(theme.getCreator())) {
            throw new UserNotHavePermissionException("Você não tem permissão para cadastrar questões nesse Tema");
        }

        String finalImageOneUrl = resolveImageUrl(imageFile1, imageOneUrl, "questions/");
        String finalImageTwoUrl = resolveImageUrl(imageFile2, imageTwoUrl, "questions/");

        QuestionRequest req = new QuestionRequest(title, imageUrl, null, null, imagesOrder);
        Question question = new Question(req, theme, creator, finalImageOneUrl, finalImageTwoUrl);
        theme.addQuestion(question);
        creator.addQuestion(question);

        questionRepository.save(question);
        log.info("questão criada (multipart): '{}' no tema id={} por {}", title, idTheme, creator.getEmail());
        return question.entityToResponse();
    }

    public QuestionResponse updateQuestionMultipart(Long id, String title, String imageUrl, String imageOneUrl,
                                                     String imageTwoUrl, String imagesOrder,
                                                     MultipartFile imageFile1, MultipartFile imageFile2,
                                                     String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(Messages.QUESTION_NOT_FOUND));

        if (user.userNotHavePermission(question.getCreator())) {
            throw new UserNotHavePermissionException("Usuário não tem permissão para atualizar essa questão");
        }

        String finalImageOneUrl = resolveImageUrl(imageFile1, imageOneUrl, "questions/");
        String finalImageTwoUrl = resolveImageUrl(imageFile2, imageTwoUrl, "questions/");

        question.setTitle(title);
        question.setImageUrl(imageUrl);
        question.setImageOneUrl(finalImageOneUrl);
        question.setImageTwoUrl(finalImageTwoUrl);
        question.setImagesOrder(imagesOrder);
        questionRepository.save(question);
        log.info("questão atualizada (multipart): id={} '{}' por {}", id, title, user.getEmail());
        return question.entityToResponse();
    }

    private String resolveImageUrl(MultipartFile file, String existingUrl, String prefix) {
        if (file != null && !file.isEmpty()) {
            return imageStorageService.upload(file, prefix);
        }
        if (existingUrl != null && !existingUrl.isBlank()) {
            return imageStorageService.upload(existingUrl);
        }
        return null;
    }

    public void removeQuestion(Long id, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(Messages.QUESTION_NOT_FOUND));

        if (user.userNotHavePermission(question.getCreator())){
            throw new UserNotHavePermissionException("Usuário não tem permissão para remover essa questão");
        }

        question.removeQuestionOfThemeList(id);
        questionRepository.delete(question);
        log.info("questão removida: id={} '{}' por {}", id, question.getTitle(), user.getEmail());
    }

    public List<QuestionResponse> find10QuestionsByThemeId(Long id){
        List<Question> questions = questionRepository.find10QuestionsByThemeId(id);

        if (questions.isEmpty()){
            throw new QuestionNotFoundException("Não existe nenhuma Questão ligada a esse Tema");
        }

        return questions.stream().map(Question::entityToResponse).toList();
    }

    // Imagens já vêm como URLs do MinIO (leves) — sem base64, sem fetch sob demanda.
    public List<QuestionQuizResponse> find10QuestionsForPlay(Long id){
        List<Question> questions = questionRepository.find10QuestionsByThemeId(id);

        if (questions.isEmpty()){
            throw new QuestionNotFoundException("Não existe nenhuma Questão ligada a esse Tema");
        }

        return questions.stream().map(Question::entityToQuizResponse).toList();
    }

    public QuestionResponse findQuestionById(Long id, String token) throws UserNotHavePermissionException {
        User loggedUser = userService.findUserByToken(token);

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(Messages.QUESTION_NOT_FOUND));

        if (loggedUser.userNotHavePermission(question.getCreator())){
            throw new UserNotHavePermissionException("Usuário não tem permissão para buscar essa questão");
        }

        return question.entityToResponse();
    }

    public Page<QuestionResponse> findQuestionByThemeId(Long id, Pageable pageable){
        Page<Question> questionPage = questionRepository.findByThemeId(id, pageable);

        if (questionPage.isEmpty()){
            throw new QuestionNotFoundException("Não existe nenhuma Questão ligada a esse Tema");
        }

        return questionPage.map(Question::entityToResponse);
    }

    public Page<QuestionResponse> findQuestionsByCreatorAndTheme(String title, Long themeId, Pageable pageable){
        themeRepository.findById(themeId)
                .orElseThrow(() -> new ThemeNotFoundException("Tema não encontrado"));

        Page<Question> questions;

        if (title.isBlank()){
            questions = questionRepository.findByThemeId(themeId, pageable);
        } else {
            questions = questionRepository.findByThemeIdAndTitleStartsWithIgnoreCase(themeId, title, pageable);
        }

        if (questions.isEmpty()){
            throw new QuestionNotFoundException("Não existe Questões criadas por esse Usuário");
        }

        return questions.map(Question::entityToResponse);
    }

    public List<QuestionResponse> findAllQuestionsByThemeId(Long themeId) {
        themeRepository.findById(themeId)
                .orElseThrow(() -> new ThemeNotFoundException("Tema não encontrado"));

        List<Question> questions = questionRepository.findByThemeId(themeId);

        if (questions.isEmpty()) {
            throw new QuestionNotFoundException("Nenhuma questão cadastrada");
        }

        return questions.stream().map(Question::entityToResponse).toList();
    }

    public QuestionResponse updateQuestion(Long id, QuestionUpdate questionUpdate, String token) throws UserNotHavePermissionException, ImageSizeLimitExceededException {
        User user = userService.findUserByToken(token);

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(Messages.QUESTION_NOT_FOUND));

        if (user.userNotHavePermission(question.getCreator())){
            throw new UserNotHavePermissionException("Usuário não tem permissão para atualizar essa questão");
        }

        validateImages(questionUpdate.imageOneUrl(), questionUpdate.imageTwoUrl());

        String imageOneUrl = imageStorageService.upload(questionUpdate.imageOneUrl());
        String imageTwoUrl = imageStorageService.upload(questionUpdate.imageTwoUrl());

        updateData(question, questionUpdate, imageOneUrl, imageTwoUrl);
        questionRepository.save(question);
        log.info("questão atualizada: id={} '{}' por {}", id, questionUpdate.title(), user.getEmail());
        return question.entityToResponse();
    }

    private void updateData(Question question, QuestionUpdate questionUpdate, String imageOneUrl, String imageTwoUrl){
        question.setTitle(questionUpdate.title());
        question.setImageUrl(questionUpdate.imageUrl());
        question.setImageOneUrl(imageOneUrl);
        question.setImageTwoUrl(imageTwoUrl);
        question.setImagesOrder(questionUpdate.imagesOrder());
    }

    // URLs já armazenadas (edição sem trocar imagem) não contam pro limite de
    // tamanho — só payloads base64 novos passam por aqui de fato.
    private void validateImages(String imageBase64One, String imageBase64Two) throws ImageSizeLimitExceededException {
        int sizeOne = isStoredUrl(imageBase64One) ? 0 : ImageValidator.decodedSizeInBytes(imageBase64One);
        int sizeTwo = isStoredUrl(imageBase64Two) ? 0 : ImageValidator.decodedSizeInBytes(imageBase64Two);

        if (sizeOne > ImageValidator.MAX_IMAGE_SIZE_BYTES || sizeTwo > ImageValidator.MAX_IMAGE_SIZE_BYTES) {
            throw new ImageSizeLimitExceededException("Cada imagem enviada deve ter no máximo 2MB");
        }
        if (sizeOne + sizeTwo > ImageValidator.MAX_TOTAL_IMAGES_SIZE_BYTES) {
            throw new ImageSizeLimitExceededException("O total das imagens enviadas deve ser de no máximo 4MB");
        }
    }

    private boolean isStoredUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    public List<QuestionResponse> find10QuestionsByThemeIdAndCreatorId(Long idTheme, String token){
        User loggedUser = userService.findUserByToken(token);

        List<Question> questions = questionRepository.find10QuestionsByThemeIdAndCreatorId(idTheme, loggedUser.getUuid());

        if (questions.isEmpty()){
            throw new QuestionNotFoundException("Não existem questões criadas por esse usuário e com esse tema");
        }

        return questions.stream().map(Question::entityToResponse).toList();
    }

}
