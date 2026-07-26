package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.ThemeFeedbackRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.feedback.ThemeFeedbackResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.ThemeFeedback;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeFeedbackRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ThemeFeedbackServiceTest {
    @Mock ThemeFeedbackRepository repository;
    @Mock ThemeRepository themeRepository;
    @Mock UserService userService;
    @InjectMocks ThemeFeedbackService service;

    User user;
    Theme theme;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User(UUID.randomUUID(), "Aluno", "u@u.com", "12345678", Role.USER);
        theme = new Theme(1L, "Tema", user);
    }

    @Test
    void create_success_savesFeedback() {
        when(userService.findUserByToken("t")).thenReturn(user);
        when(themeRepository.findById(1L)).thenReturn(Optional.of(theme));
        when(repository.save(any(ThemeFeedback.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ThemeFeedbackResponse res = service.create(1L, new ThemeFeedbackRequest("otimo tema"), "t");

        assertEquals("otimo tema", res.text());
        assertEquals(1L, res.themeId());
        assertEquals(user.getUuid(), res.userUuid());
        verify(repository).save(any(ThemeFeedback.class));
    }

    @Test
    void create_themeNotFound_throws() {
        when(userService.findUserByToken("t")).thenReturn(user);
        when(themeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ThemeNotFoundException.class,
                () -> service.create(99L, new ThemeFeedbackRequest("texto qualquer"), "t"));
    }

    @Test
    void listByTheme_returnsAllOrdered() {
        ThemeFeedback f1 = new ThemeFeedback(theme, user, "primeiro");
        ThemeFeedback f2 = new ThemeFeedback(theme, user, "segundo");
        when(repository.findByTheme_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(f1, f2));

        List<ThemeFeedbackResponse> res = service.listByTheme(1L);
        assertEquals(2, res.size());
    }

    @Test
    void count_returnsRepositoryCount() {
        when(repository.countByTheme_Id(1L)).thenReturn(5L);
        assertEquals(5L, service.count(1L));
    }
}
