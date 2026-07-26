package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StudentName;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.ThemeName;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatisticService {
    private final StatisticRepository statisticRepository;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;

    public StatisticService(StatisticRepository statisticRepository, ThemeRepository themeRepository,
                            UserRepository userRepository) {
        this.statisticRepository = statisticRepository;
        this.themeRepository = themeRepository;
        this.userRepository = userRepository;
    }

    public StatisticPerConclusion getById(Long id) {
        return statisticRepository.findById(id).orElse(null);
    }

    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }

    public StatisticResponse insertStatistic(StatisticRequest statisticRequest) {
        return insertStatistic(statisticRequest, null);
    }

    public StatisticResponse insertStatistic(StatisticRequest statisticRequest, UUID studentUuid) {
        Theme theme = themeRepository.findByNameIgnoreCase(statisticRequest.themeName());

        if (theme == null) {
            throw new ThemeNotFoundException("Tema não encontrado");
        }

        UUID creatorId = theme.getCreator().getUuid();
        StatisticPerConclusion statistic = new StatisticPerConclusion(statisticRequest);
        statistic.setCreatorId(creatorId);
        statistic.setStudentUuid(studentUuid);

        statisticRepository.save(statistic);

        return statistic.entityToResponse();
    }

    /**
     * Sentinelas para o filtro de datas: quando não informadas, usamos limites
     * extremos em vez de null porque o driver Postgres não infere o tipo de
     * parâmetro null (envia como bytea e falha ao comparar com timestamp).
     */
    static final LocalDateTime MIN_DATE_TIME = LocalDateTime.of(1900, 1, 1, 0, 0);
    static final LocalDateTime MAX_DATE_TIME = LocalDateTime.of(9999, 12, 31, 0, 0);

    /** Início do dia informado (ou sentinela mínima quando não filtrado). */
    static LocalDateTime startOfDayOrMin(LocalDate startDate) {
        return startDate == null ? MIN_DATE_TIME : startDate.atStartOfDay();
    }

    /**
     * endDate é inclusivo: devolve o início do dia seguinte, para a comparação
     * `< endDateTimeExclusive` cobrir todas as horas do dia informado.
     */
    static LocalDateTime endOfDayExclusiveOrMax(LocalDate endDate) {
        return endDate == null ? MAX_DATE_TIME : endDate.plusDays(1).atStartOfDay();
    }

    public Page<StatisticResponse> findAllStatisticsByCreator(Pageable pageable, UUID creatorId, String studentName, String themeName, LocalDate startDate, LocalDate endDate, GameMode gameMode) {
        LocalDateTime start = startOfDayOrMin(startDate);
        LocalDateTime endExclusive = endOfDayExclusiveOrMax(endDate);
        Page<StatisticPerConclusion> statistic = isAdmin(creatorId)
                ? statisticRepository.findByFiltersForAdmin(pageable, studentName, themeName, start, endExclusive, gameMode)
                : statisticRepository.findByCreatorIdAndFilters(pageable, creatorId, studentName, themeName, start, endExclusive, gameMode);

        return statistic.map(StatisticPerConclusion::entityToResponse);
    }

    public List<StatisticResponse> findAllStatisticsByCreatorForChart(UUID creatorId, String studentName, String themeName, LocalDate startDate, LocalDate endDate, GameMode gameMode) {
        return findAllStatisticsByCreator(Pageable.unpaged(), creatorId, studentName, themeName, startDate, endDate, gameMode).getContent();
    }

    public List<StudentName> findDistinctStudentNameByCreatorId(UUID creatorId) {
        Set<StudentName> seenThemesAndStudents = new HashSet<>();

        List<StatisticPerConclusion> source = isAdmin(creatorId)
                ? statisticRepository.findAll()
                : statisticRepository.findByCreatorId(creatorId);

        return source
                .stream()
                .map(sts -> new StudentName(sts.getStudentName()))
                .filter(seenThemesAndStudents::add)
                .sorted(Comparator.comparing(student -> student.studentName().toLowerCase()))
                .toList();
    }

    public List<ThemeName> findDistinctThemeNameByCreatorId(UUID creatorId) {
        Set<ThemeName> seenThemesAndStudents = new HashSet<>();

        List<StatisticPerConclusion> source = isAdmin(creatorId)
                ? statisticRepository.findAll()
                : statisticRepository.findByCreatorId(creatorId);

        return source
                .stream()
                .map(sts -> new ThemeName(sts.getThemeName()))
                .filter(seenThemesAndStudents::add)
                .sorted(Comparator.comparing(theme -> theme.themeName().toLowerCase()))
                .toList();
    }
}
