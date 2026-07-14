package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.ThemeName;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StatisticServiceTest {
    @Mock StatisticRepository statisticRepository;
    @Mock ThemeRepository themeRepository;
    @Mock UserRepository userRepository;
    @InjectMocks StatisticService statisticService;

    User creator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creator = new User(UUID.randomUUID(), "Creator", "c@c.com", "12345678", Role.USER);
    }

    @Test
    void insertStatistic_themeNotFound_throws() {
        when(themeRepository.findByNameIgnoreCase("Tema")).thenReturn(null);

        assertThrows(ThemeNotFoundException.class,
                () -> statisticService.insertStatistic(new StatisticRequest("Aluno", "Tema", 80.0)));
    }

    @Test
    void insertStatistic_success_setsCreatorAndSaves() {
        Theme theme = new Theme(1L, "Tema", creator);
        when(themeRepository.findByNameIgnoreCase("Tema")).thenReturn(theme);

        StatisticResponse response =
                statisticService.insertStatistic(new StatisticRequest("Aluno", "Tema", 80.0));

        assertNotNull(response);
        verify(statisticRepository).save(any(StatisticPerConclusion.class));
    }

    @Test
    void findAllStatisticsByCreator_regularUser_usesCreatorScopedQuery() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        Page<StatisticPerConclusion> page = new PageImpl<>(List.of(sampleStat()));
        when(statisticRepository.findByCreatorIdAndFilters(any(), eq(creator.getUuid()), anyString(), anyString(), any(), any()))
                .thenReturn(page);

        Page<StatisticResponse> result = statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), creator.getUuid(), "", "", null, null);

        assertEquals(1, result.getTotalElements());
        verify(statisticRepository, never()).findByFiltersForAdmin(any(), anyString(), anyString(), any(), any());
    }

    @Test
    void findAllStatisticsByCreator_admin_usesGlobalQuery() {
        User admin = new User(UUID.randomUUID(), "Admin", "adm@a.com", "12345678", Role.ADMIN);
        when(userRepository.findById(admin.getUuid())).thenReturn(Optional.of(admin));
        Page<StatisticPerConclusion> page = new PageImpl<>(List.of(sampleStat()));
        when(statisticRepository.findByFiltersForAdmin(any(), anyString(), anyString(), any(), any()))
                .thenReturn(page);

        Page<StatisticResponse> result = statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), admin.getUuid(), "", "", null, null);

        assertEquals(1, result.getTotalElements());
        verify(statisticRepository, never()).findByCreatorIdAndFilters(any(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void findDistinctThemeNameByCreatorId_dedupsAndSorts() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        when(statisticRepository.findByCreatorId(creator.getUuid()))
                .thenReturn(List.of(sampleStat("Zoo"), sampleStat("Arte"), sampleStat("Arte")));

        List<ThemeName> themes = statisticService.findDistinctThemeNameByCreatorId(creator.getUuid());

        assertEquals(2, themes.size());
        assertEquals("Arte", themes.get(0).themeName()); // ordenado alfabeticamente
    }

    private StatisticPerConclusion sampleStat() {
        return sampleStat("Tema");
    }

    private StatisticPerConclusion sampleStat(String themeName) {
        StatisticPerConclusion stat = new StatisticPerConclusion("Aluno", themeName, 80.0);
        stat.setCreatorId(creator.getUuid());
        stat.setDate(LocalDate.now());
        return stat;
    }
}
