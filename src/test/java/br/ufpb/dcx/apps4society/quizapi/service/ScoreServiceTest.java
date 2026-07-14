package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Score;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.ScoreRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ScoreNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScoreServiceTest {
    @Mock ScoreRepository scoreRepository;
    @Mock UserRepository userRepository;
    @Mock ThemeRepository themeRepository;
    @InjectMocks ScoreService scoreService;

    User user;
    Theme theme;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User(UUID.randomUUID(), "User", "u@u.com", "12345678", Role.USER);
        theme = new Theme(1L, "Tema", user);
    }

    @Test
    void insertScore_success() {
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(themeRepository.findById(1L)).thenReturn(Optional.of(theme));

        ScoreResponse response = scoreService.insertScore(new ScoreRequest(5, 30), user.getUuid(), 1L);

        assertNotNull(response);
        verify(scoreRepository, times(1)).save(any(Score.class));
    }

    @Test
    void insertScore_userNotFound() {
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> scoreService.insertScore(new ScoreRequest(5, 30), user.getUuid(), 1L));
    }

    @Test
    void insertScore_themeNotFound() {
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        when(themeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ThemeNotFoundException.class,
                () -> scoreService.insertScore(new ScoreRequest(5, 30), user.getUuid(), 1L));
    }

    @Test
    void findRankingByTheme_empty_throws() {
        when(scoreRepository.findByTheme(1L)).thenReturn(List.of());

        assertThrows(ScoreNotFoundException.class, () -> scoreService.findRankingByTheme(1L));
    }

    @Test
    void findRankingByTheme_returnsMappedList() {
        Score score = new Score(3, 10, user, theme);
        when(scoreRepository.findByTheme(1L)).thenReturn(List.of(score));

        List<ScoreResponse> ranking = scoreService.findRankingByTheme(1L);

        assertEquals(1, ranking.size());
    }
}
