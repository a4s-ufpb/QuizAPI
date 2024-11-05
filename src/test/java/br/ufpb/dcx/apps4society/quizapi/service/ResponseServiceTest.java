package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseDTO;
import br.ufpb.dcx.apps4society.quizapi.entity.*;
import br.ufpb.dcx.apps4society.quizapi.mock.*;
import br.ufpb.dcx.apps4society.quizapi.repository.AlternativeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ResponseRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.security.TokenProvider;
import br.ufpb.dcx.apps4society.quizapi.service.exception.*;
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
import static org.mockito.Mockito.mock;

class ResponseServiceTest {
    MockUser mockUser;
    MockQuestion mockQuestion;
    MockTheme mockTheme;
    MockAlternative mockAlternative;
    MockResponse mockResponse;
    @Mock
    ResponseRepository responseRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    QuestionRepository questionRepository;
    @Mock
    AlternativeRepository alternativeRepository;
    @Mock
    TokenProvider tokenProvider;
    @InjectMocks
    ResponseService responseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = new MockUser();
        mockQuestion = new MockQuestion();
        mockTheme = new MockTheme();
        mockAlternative = new MockAlternative();
        mockResponse = new MockResponse();
    }

    @Test
    void insertResponse() {
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Alternative alternative = mockAlternative.mockEntity(1, true, question);

        Mockito.lenient().when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        Mockito.lenient().when(alternativeRepository.findById(alternative.getId())).thenReturn(Optional.of(alternative));

        ResponseDTO result = responseService.insertResponse(user.getUuid(), question.getId(), alternative.getId());

        assertNotNull(result);
        assertNotNull(result.user());
        assertNotNull(result.question());
        assertNotNull(result.alternative());
    }

    @Test
    void insertResponseWithUserNotFound() {
        User user = mockUser.mockEntity(1);
        User falseUser = mockUser.mockEntity(2);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Alternative alternative = mockAlternative.mockEntity(1, true, question);

        Mockito.lenient().when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        Mockito.lenient().when(alternativeRepository.findById(alternative.getId())).thenReturn(Optional.of(alternative));

        Exception e = assertThrows(UserNotFoundException.class, () ->{
            responseService.insertResponse(falseUser.getUuid(), question.getId(), alternative.getId());
        });

        assertEquals(e.getMessage(), Messages.USER_NOT_FOUND);
    }

    @Test
    void insertResponseWithQuestionNotFound() {
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Question falseQuestion = mockQuestion.mockEntity(2);
        Alternative alternative = mockAlternative.mockEntity(1, true, question);

        Mockito.lenient().when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        Mockito.lenient().when(alternativeRepository.findById(alternative.getId())).thenReturn(Optional.of(alternative));

        Exception e = assertThrows(QuestionNotFoundException.class, () ->{
            responseService.insertResponse(user.getUuid(), falseQuestion.getId(), alternative.getId());
        });

        assertEquals(e.getMessage(), Messages.QUESTION_NOT_FOUND);
    }

    @Test
    void insertResponseWithAlternativeNotFound() {
        User user = mockUser.mockEntity(1);
        Question question = mockQuestion.mockEntity(1, new Theme(), user);
        Alternative alternative = mockAlternative.mockEntity(1, true, question);
        Alternative falseAlternative = mockAlternative.mockEntity(2);

        Mockito.lenient().when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        Mockito.lenient().when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        Mockito.lenient().when(alternativeRepository.findById(alternative.getId())).thenReturn(Optional.of(alternative));

        Exception e = assertThrows(AlternativeNotFoundException.class, () ->{
            responseService.insertResponse(user.getUuid(), question.getId(), falseAlternative.getId());
        });

        assertEquals(e.getMessage(), Messages.ALTERNATIVE_NOT_FOUND);
    }

    @Test
    void findResponsesByUser() {
        User user = mockUser.mockEntity(1);

        Mockito.lenient().when(tokenProvider.getSubjectByToken(MockUser.MOCK_TOKEN)).thenReturn(user.getEmail());
        Mockito.lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        Mockito.lenient().when(responseService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);

        Pageable pageable = mock(Pageable.class);
        List<Response> responseList = mockResponse.mockList(5);
        Page<Response> responsePage = new PageImpl<>(responseList);

        Mockito.lenient().when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        Mockito.lenient().when(responseRepository.findByUser(pageable, user)).thenReturn(responsePage);

        Page<ResponseDTO> result = responseService.findResponsesByUser(pageable, MockUser.MOCK_TOKEN);

        assertEquals(responseList.size(), result.getTotalElements());
    }

    @Test
    void findResponsesByUserNotFound() {
        User user = mockUser.mockEntity(1);

        Mockito.lenient().when(tokenProvider.getSubjectByToken(MockUser.MOCK_TOKEN)).thenReturn(user.getEmail());
        Mockito.lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        Mockito.lenient().when(responseService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);

        User falseUser = mockUser.mockEntity(2);
        Pageable pageable = mock(Pageable.class);

        Mockito.lenient().when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));

        Exception e = assertThrows(InvalidUserException.class, () ->{
            responseService.findResponsesByUser(pageable, "invalidToken");
        });

        assertEquals(e.getMessage(), Messages.INVALID_USER);
    }

    @Test
    void findResponsesListIsEmpty() {
        User user = mockUser.mockEntity(1);

        Mockito.lenient().when(tokenProvider.getSubjectByToken(MockUser.MOCK_TOKEN)).thenReturn(user.getEmail());
        Mockito.lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        Mockito.lenient().when(responseService.findUserByToken(MockUser.MOCK_TOKEN)).thenReturn(user);

        Pageable pageable = mock(Pageable.class);
        List<Response> responseList = new ArrayList<>();
        Page<Response> responsePage = new PageImpl<>(responseList);

        Mockito.lenient().when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        Mockito.lenient().when(responseRepository.findByUser(pageable, user)).thenReturn(responsePage);

        Exception e = assertThrows(ResponseNotFoundException.class, () ->{
            responseService.findResponsesByUser(pageable, MockUser.MOCK_TOKEN);
        });

        assertEquals(e.getMessage(), Messages.USER_NOT_HAVE_RESPONSES);
    }
}