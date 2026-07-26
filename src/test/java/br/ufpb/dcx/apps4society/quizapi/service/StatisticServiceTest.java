package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.ThemeName;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        assertEquals(GameMode.SINGLE_PLAYER, response.gameMode());
        verify(statisticRepository).save(any(StatisticPerConclusion.class));
    }

    @Test
    void insertStatistic_multiplayerGameMode_persistedInEntity() {
        Theme theme = new Theme(1L, "Tema", creator);
        when(themeRepository.findByNameIgnoreCase("Tema")).thenReturn(theme);

        StatisticResponse response = statisticService.insertStatistic(
                new StatisticRequest("Aluno", "Tema", 80.0, GameMode.MULTIPLAYER),
                UUID.randomUUID()
        );

        assertEquals(GameMode.MULTIPLAYER, response.gameMode());
        ArgumentCaptor<StatisticPerConclusion> cap = ArgumentCaptor.forClass(StatisticPerConclusion.class);
        verify(statisticRepository).save(cap.capture());
        assertEquals(GameMode.MULTIPLAYER, cap.getValue().getGameMode());
    }

    @Test
    void findAllStatisticsByCreator_regularUser_usesCreatorScopedQuery() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        Page<StatisticPerConclusion> page = new PageImpl<>(List.of(sampleStat()));
        when(statisticRepository.findByCreatorIdAndFilters(
                any(), eq(creator.getUuid()), anyString(), anyString(), any(), any(), any()))
                .thenReturn(page);

        Page<StatisticResponse> result = statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), creator.getUuid(), "", "", null, null, null);

        assertEquals(1, result.getTotalElements());
        verify(statisticRepository, never())
                .findByFiltersForAdmin(any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void findAllStatisticsByCreator_admin_usesGlobalQuery() {
        User admin = new User(UUID.randomUUID(), "Admin", "adm@a.com", "12345678", Role.ADMIN);
        when(userRepository.findById(admin.getUuid())).thenReturn(Optional.of(admin));
        Page<StatisticPerConclusion> page = new PageImpl<>(List.of(sampleStat()));
        when(statisticRepository.findByFiltersForAdmin(
                any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(page);

        Page<StatisticResponse> result = statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), admin.getUuid(), "", "", null, null, null);

        assertEquals(1, result.getTotalElements());
        verify(statisticRepository, never()).findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void findAllStatisticsByCreator_endDateFilter_passesEndDatePlusOne() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        Page<StatisticPerConclusion> page = new PageImpl<>(List.of(sampleStat()));
        when(statisticRepository.findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(page);

        LocalDate end = LocalDate.of(2025, 3, 10);
        statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), creator.getUuid(), "", "", null, end, null);

        // endDate inclusivo vira "< início do dia seguinte" (cobre as horas do dia).
        ArgumentCaptor<LocalDateTime> endExclusiveCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(statisticRepository).findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), any(), endExclusiveCap.capture(), any());
        assertEquals(end.plusDays(1).atStartOfDay(), endExclusiveCap.getValue());
    }

    @Test
    void findAllStatisticsByCreator_startDateFilter_passesStartOfDay() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        when(statisticRepository.findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleStat())));

        LocalDate start = LocalDate.of(2025, 3, 1);
        statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), creator.getUuid(), "", "", start, null, null);

        ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(statisticRepository).findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), startCap.capture(), any(), any());
        assertEquals(start.atStartOfDay(), startCap.getValue());
    }

    @Test
    void findAllStatisticsByCreator_nullDates_passSentinels() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        Page<StatisticPerConclusion> page = new PageImpl<>(List.of(sampleStat()));
        when(statisticRepository.findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(page);

        statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), creator.getUuid(), "", "", null, null, null);

        ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endExclusiveCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(statisticRepository).findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), startCap.capture(), endExclusiveCap.capture(), isNull());
        assertNotNull(startCap.getValue());
        assertNotNull(endExclusiveCap.getValue());
        // Sentinelas usam ano bem no passado / futuro, cobrindo qualquer data real.
        assertTrue(startCap.getValue().getYear() <= 1900);
        assertTrue(endExclusiveCap.getValue().getYear() >= 9000);
    }

    @Test
    void findAllStatisticsByCreator_gameModeMultiplayer_propagates() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        when(statisticRepository.findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        statisticService.findAllStatisticsByCreator(
                Pageable.unpaged(), creator.getUuid(), "", "", null, null, GameMode.MULTIPLAYER);

        verify(statisticRepository).findByCreatorIdAndFilters(
                any(), any(), anyString(), anyString(), any(), any(), eq(GameMode.MULTIPLAYER));
    }

    @Test
    void findDistinctThemeNameByCreatorId_dedupsAndSorts() {
        when(userRepository.findById(creator.getUuid())).thenReturn(Optional.of(creator));
        when(statisticRepository.findByCreatorId(creator.getUuid()))
                .thenReturn(List.of(sampleStat("Zoo"), sampleStat("Arte"), sampleStat("Arte")));

        List<ThemeName> themes = statisticService.findDistinctThemeNameByCreatorId(creator.getUuid());

        assertEquals(2, themes.size());
        assertEquals("Arte", themes.get(0).themeName());
    }

    private StatisticPerConclusion sampleStat() {
        return sampleStat("Tema");
    }

    private StatisticPerConclusion sampleStat(String themeName) {
        StatisticPerConclusion stat = new StatisticPerConclusion("Aluno", themeName, 80.0);
        stat.setCreatorId(creator.getUuid());
        stat.setDate(LocalDateTime.now());
        return stat;
    }
}
