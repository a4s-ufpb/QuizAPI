package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.StatisticFeedbackRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.feedback.StatisticFeedbackResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticFeedback;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticFeedbackRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StatisticFeedbackServiceTest {
    @Mock StatisticFeedbackRepository repository;
    @Mock StatisticService statisticService;
    @Mock UserService userService;
    @InjectMocks StatisticFeedbackService service;

    User creator;
    User other;
    StatisticPerConclusion stat;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creator = new User(UUID.randomUUID(), "Creator", "c@c.com", "12345678", Role.USER);
        other = new User(UUID.randomUUID(), "Other", "o@o.com", "12345678", Role.USER);
        stat = new StatisticPerConclusion("Aluno", "Tema", 80.0);
        stat.setCreatorId(creator.getUuid());
    }

    @Test
    void create_asCreator_saves() throws UserNotHavePermissionException {
        when(userService.findUserByToken("t")).thenReturn(creator);
        when(statisticService.getById(1L)).thenReturn(stat);
        when(repository.save(any(StatisticFeedback.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StatisticFeedbackResponse res =
                service.create(1L, new StatisticFeedbackRequest("bom trabalho"), "t");

        assertEquals("bom trabalho", res.text());
        verify(repository).save(any(StatisticFeedback.class));
    }

    @Test
    void create_notCreator_throwsPermission() {
        when(userService.findUserByToken("t")).thenReturn(other);
        when(statisticService.getById(1L)).thenReturn(stat);

        assertThrows(UserNotHavePermissionException.class,
                () -> service.create(1L, new StatisticFeedbackRequest("texto"), "t"));

        verify(repository, never()).save(any());
    }

    @Test
    void listMine_queriesByStudentUuid() {
        when(userService.findUserByToken("t")).thenReturn(other);
        when(repository.findByStudentUuid(other.getUuid())).thenReturn(List.of());

        service.listMine("t");
        verify(repository).findByStudentUuid(other.getUuid());
    }

    @Test
    void listByCreator_queriesByCreatorUuid() {
        when(userService.findUserByToken("t")).thenReturn(creator);
        when(repository.findByCreator_UuidOrderByCreatedAtDesc(creator.getUuid()))
                .thenReturn(List.of());

        service.listByCreator("t");
        verify(repository).findByCreator_UuidOrderByCreatedAtDesc(creator.getUuid());
    }
}
