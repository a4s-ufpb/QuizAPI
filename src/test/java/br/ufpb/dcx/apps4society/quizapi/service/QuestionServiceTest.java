package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionUpdate;
import br.ufpb.dcx.apps4society.quizapi.mock.MockQuestion;
import br.ufpb.dcx.apps4society.quizapi.mock.MockTheme;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.mock.MockUser;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ImageSizeLimitExceededException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.QuestionNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class QuestionServiceTest {
    MockQuestion mockQuestion;
    MockUser mockUser;
    MockTheme mockTheme;
    @Mock
    QuestionRepository questionRepository;
    @Mock
    ThemeRepository themeRepository;
    @Mock
    UserService userService;
    @InjectMocks
    QuestionService questionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockQuestion = new MockQuestion();
        mockUser = new MockUser();
        mockTheme = new MockTheme();
    }

    @Test
    void insertQuestion() throws UserNotHavePermissionException, ImageSizeLimitExceededException {
        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        User creator = mockUser.mockEntity(1);
        Theme theme = mockTheme.mockEntity(1, creator);
        Question question = mockQuestion.mockEntity(1);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(creator);
        Mockito.lenient().when(themeRepository.findById(theme.getId())).thenReturn(Optional.of(theme));
        Mockito.lenient().when(questionRepository.save(question)).thenReturn(question);

        QuestionResponse result = questionService.insertQuestion(questionRequest, theme.getId(), MockUser.MOCK_TOKEN);

        assertNotNull(result);
        assertNotNull(result.imageUrl());
        assertEquals("Question", result.title());
    }

    @Test
    void insertQuestionWithThemeNotFound() {
        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        User creator = mockUser.mockEntity(1);
        Theme theme = mockTheme.mockEntity(1);
        Theme falseTheme = mockTheme.mockEntity(2);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(creator);
        Mockito.lenient().when(themeRepository.findById(theme.getId())).thenReturn(Optional.of(theme));

        Exception e = assertThrows(ThemeNotFoundException.class, () ->{
            questionService.insertQuestion(questionRequest, falseTheme.getId(), MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.THEME_NOT_FOUND);
    }

    @Test
    void removeQuestion() {
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        assertDoesNotThrow(() -> questionService.removeQuestion(question.getId(), MockUser.MOCK_TOKEN));

        verify(questionRepository, times(1)).delete(question);
    }

    @Test
    void removeQuestionNotFound() {
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Question falseQuestion = mockQuestion.mockEntity(2);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(QuestionNotFoundException.class, () ->{
            questionService.removeQuestion(falseQuestion.getId(), MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.QUESTION_NOT_FOUND);
    }

    @Test
    void removeQuestionNotHavePermission() {
        User user = mockUser.mockEntity(1);
        User falseUser = mockUser.mockEntity(2);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(falseUser);
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(UserNotHavePermissionException.class, () ->{
            questionService.removeQuestion(question.getId(), MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.USER_NOT_HAVE_PERMISSION_FOR_REMOVE_QUESTION);
    }

    @Test
    void find10QuestionsByThemeId() {
        Theme theme = mockTheme.mockEntity(1);
        List<Question> questionList = mockQuestion.mockList(10);
        Mockito.lenient().when(questionRepository.find10QuestionsByThemeId(theme.getId())).thenReturn(questionList);
        List<QuestionResponse> result = questionService.find10QuestionsByThemeId(theme.getId());

        assertEquals(10, result.size());
        assertEquals("Question", result.getFirst().title());
    }

    @Test
    void find10QuestionsByThemeIdNotFound() {
        Theme theme = mockTheme.mockEntity(1);
        List<Question> questionList = new ArrayList<>();
        Mockito.lenient().when(questionRepository.find10QuestionsByThemeId(theme.getId())).thenReturn(questionList);

        Exception e = assertThrows(QuestionNotFoundException.class, () -> {
            questionService.find10QuestionsByThemeId(theme.getId());
        });

        assertEquals(e.getMessage(), Messages.NOT_FOUND_QUESTIONS_BY_THEME);
    }

    @Test
    void findQuestionById() throws UserNotHavePermissionException {
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1,new Theme(), user);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);

        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        QuestionResponse result = questionService.findQuestionById(question.getId(), MockUser.MOCK_TOKEN);

        assertNotNull(result);
        assertNotNull(result.imageUrl());
        assertEquals("Question", result.title());
    }

    @Test
    void findQuestionByIdNotFound() {
        Question question = mockQuestion.mockEntity(1);
        Question falseQuestion = mockQuestion.mockEntity(2);

        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(QuestionNotFoundException.class, () ->{
            questionService.findQuestionById(falseQuestion.getId(), MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.QUESTION_NOT_FOUND);
    }

    @Test
    void findQuestionByThemeId() {
        Pageable pageable = mock(Pageable.class);
        Theme theme = mockTheme.mockEntity(1);

        List<Question> questionList = mockQuestion.mockList(5);
        Page<Question> questionPage = new PageImpl<>(questionList);

        Mockito.lenient().when(questionRepository.findByThemeId(theme.getId(), pageable)).thenReturn(questionPage);

        List<QuestionResponse> result = questionService.findQuestionByThemeId(theme.getId(), pageable).toList();

        assertEquals(5, result.size());
        assertNotNull(result.getFirst().imageUrl());
        assertEquals("Question", result.getFirst().title());
    }

    @Test
    void findQuestionByThemeNotFound() {
        Pageable pageable = mock(Pageable.class);
        Theme theme = mockTheme.mockEntity(1);

        List<Question> questionList = new ArrayList<>();
        Page<Question> questionPage = new PageImpl<>(questionList);

        Mockito.lenient().when(questionRepository.findByThemeId(theme.getId(), pageable)).thenReturn(questionPage);

        Exception e = assertThrows(QuestionNotFoundException.class, () ->{
            questionService.findQuestionByThemeId(theme.getId(), pageable);
        });

        assertEquals(e.getMessage(), Messages.NOT_FOUND_QUESTIONS_BY_THEME);
    }

    @Test
    void updateQuestion() throws UserNotHavePermissionException, ImageSizeLimitExceededException {
        QuestionUpdate questionUpdate = mockQuestion.mockQuestionUpdate();
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1,new Theme(), user);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        Mockito.lenient().when(questionRepository.save(question)).thenReturn(question);

        QuestionResponse result = questionService.updateQuestion(question.getId(), questionUpdate, MockUser.MOCK_TOKEN);

        assertNotNull(result);
        assertEquals("Novo titulo", result.title());
        assertEquals("http://nova.com", result.imageUrl());
    }

    @Test
    void updateQuestionNotFound(){
        QuestionUpdate questionUpdate = mockQuestion.mockQuestionUpdate();
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1,new Theme(), user);
        Question falseQuestion = mockQuestion.mockEntity(2);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(QuestionNotFoundException.class, () ->{
            questionService.updateQuestion(falseQuestion.getId(), questionUpdate, MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.QUESTION_NOT_FOUND);
    }

    @Test
    void updateQuestionNotHavePermission(){
        QuestionUpdate questionUpdate = mockQuestion.mockQuestionUpdate();
        User user = mockUser.mockEntity(1);
        User falseUser = mockUser.mockEntity(2);
        Question question = mockQuestion.mockEntity(1,new Theme(), user);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(falseUser);
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(UserNotHavePermissionException.class, () ->{
            questionService.updateQuestion(question.getId(), questionUpdate, MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.USER_NOT_HAVE_PERMISSION_FOR_UPDATE_QUESTION);
    }
}