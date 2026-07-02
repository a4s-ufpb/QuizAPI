package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeUpdate;
import br.ufpb.dcx.apps4society.quizapi.mock.MockAlternative;
import br.ufpb.dcx.apps4society.quizapi.mock.MockQuestion;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Alternative;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.mock.MockUser;
import br.ufpb.dcx.apps4society.quizapi.repository.AlternativeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.*;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AlternativeServiceTest {
    MockAlternative mockAlternative;
    MockQuestion mockQuestion;
    MockUser mockUser;
    @Mock
    AlternativeRepository alternativeRepository;
    @Mock
    QuestionRepository questionRepository;
    @Mock
    UserService userService;
    @InjectMocks
    AlternativeService alternativeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockAlternative = new MockAlternative();
        mockQuestion = new MockQuestion();
        mockUser = new MockUser();
    }

    @Test
    void insertAlternative() throws FalseAlternativesOnlyException, AlternativeCorrectDuplicateException, LimitOfAlternativesException {
        Alternative alternative = mockAlternative.mockEntity(1);

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), new User());

        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        Mockito.lenient().when(alternativeRepository.save(alternative)).thenReturn(alternative);

        AlternativeResponse result = alternativeService.insertAlternative(alternativeRequest, question.getId());

        assertNotNull(result);
        assertEquals("Alternative", result.text());
    }

    @Test
    void insertAlternativeWithQuestionNotFound(){
        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), new User());
        Question falseQuestion = mockQuestion.mockEntity(2);

        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(QuestionNotFoundException.class, () ->{
            alternativeService.insertAlternative(alternativeRequest, falseQuestion.getId());
        });

        assertEquals(e.getMessage(), Messages.QUESTION_NOT_FOUND);
    }

    @Test
    void insertAlternativesWithMoreThanTheLimit(){
        Question question = mockQuestion.mockEntity(1, new Theme(), new User());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeRequest alternativeTrue = mockAlternative.mockAlternativeRequest(true);

        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(LimitOfAlternativesException.class, () ->{
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeTrue, question.getId());
            alternativeService.insertAlternative(alternativeTrue, question.getId());
        });

        assertEquals(e.getMessage(), Messages.LIMIT_MAX_OF_ALTERNATIVES);
    }

    @Test
    void insertAlternativesCorrectDuplicate(){
        Question question = mockQuestion.mockEntity(1, new Theme(), new User());

        AlternativeRequest alternativeTrue1 = mockAlternative.mockAlternativeRequest(true);
        AlternativeRequest alternativeTrue2 = mockAlternative.mockAlternativeRequest(true);

        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(AlternativeCorrectDuplicateException.class, () ->{
            alternativeService.insertAlternative(alternativeTrue1, question.getId());
            alternativeService.insertAlternative(alternativeTrue2, question.getId());
        });

        assertEquals(e.getMessage(), Messages.ALTERNATIVE_CORRECT_DUPLICATE);
    }

    @Test
    void insertAlternativesFalsesOnly(){
        Question question = mockQuestion.mockEntity(1, new Theme(), new User());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);

        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));

        Exception e = assertThrows(FalseAlternativesOnlyException.class, () ->{
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
            alternativeService.insertAlternative(alternativeRequest, question.getId());
        });

        assertEquals(e.getMessage(), Messages.ALTERNATIVES_FALSES_ONLY);
    }


    @Test
    void updateAlternative() throws UserNotHavePermissionException {
        AlternativeUpdate alternativeUpdate = mockAlternative.mockAlternativeUpdate();
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Alternative alternative = mockAlternative.mockEntity(1, false, question);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);
        Mockito.lenient().when(alternativeRepository.findById(alternative.getId())).thenReturn(Optional.of(alternative));
        Mockito.lenient().when(alternativeRepository.save(alternative)).thenReturn(alternative);

        AlternativeResponse result = alternativeService.updateAlternative(alternative.getId(), alternativeUpdate, MockUser.MOCK_TOKEN);

        assertNotNull(result);
        assertEquals("Nova Alternativa", result.text());
    }

    @Test
    void updateAlternativeNotFound(){
        AlternativeUpdate alternativeUpdate = mockAlternative.mockAlternativeUpdate();
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Alternative alternative = mockAlternative.mockEntity(1, false, question);
        Alternative falseAlternative = mockAlternative.mockEntity(2);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);
        Mockito.lenient().when(alternativeRepository.findById(alternative.getId())).thenReturn(Optional.of(alternative));

        Exception e = assertThrows(AlternativeNotFoundException.class, () ->{
            alternativeService.updateAlternative(falseAlternative.getId(), alternativeUpdate, MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.ALTERNATIVE_NOT_FOUND);
    }

    @Test
    void updateAlternativeNotHavePermission(){
        AlternativeUpdate alternativeUpdate = mockAlternative.mockAlternativeUpdate();

        User user = mockUser.mockEntity(1);
        User falseUser = mockUser.mockEntity(2);

        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Alternative alternative = mockAlternative.mockEntity(1, false, question);

        Mockito.lenient().when(userService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(falseUser);
        Mockito.lenient().when(alternativeRepository.findById(alternative.getId())).thenReturn(Optional.of(alternative));

        Exception e = assertThrows(UserNotHavePermissionException.class, () ->{
            alternativeService.updateAlternative(alternative.getId(), alternativeUpdate, MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.USER_NOT_HAVE_PERMISSION_FOR_UPDATE_ALTERNATIVE);
    }
}