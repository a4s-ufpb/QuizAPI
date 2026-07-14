package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StudentName;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.ThemeName;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    /** ADMIN enxerga estatísticas (e listas de filtros) de todos os criadores. */
    private boolean isAdmin(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);
    }

    public StatisticResponse insertStatistic(StatisticRequest statisticRequest) {
        Theme theme = themeRepository.findByNameIgnoreCase(statisticRequest.themeName());

        if (theme == null) {
            throw new ThemeNotFoundException("Tema não encontrado");
        }

        UUID creatorId = theme.getCreator().getUuid();
        StatisticPerConclusion statistic = new StatisticPerConclusion(statisticRequest);
        statistic.setCreatorId(creatorId);

        statisticRepository.save(statistic);

        return statistic.entityToResponse();
    }

    public Page<StatisticResponse> findAllStatisticsByCreator(Pageable pageable, UUID creatorId, String studentName, String themeName, LocalDate startDate, LocalDate endDate) {
        // ADMIN enxerga as estatísticas de todos os criadores.
        Page<StatisticPerConclusion> statistic = isAdmin(creatorId)
                ? statisticRepository.findByFiltersForAdmin(pageable, studentName, themeName, startDate, endDate)
                : statisticRepository.findByCreatorIdAndFilters(pageable, creatorId, studentName, themeName, startDate, endDate);

        return statistic.map(StatisticPerConclusion::entityToResponse);
    }

    public List<StatisticResponse> findAllStatisticsByCreatorForChart(UUID creatorId, String studentName, String themeName, LocalDate startDate, LocalDate endDate) {
        return findAllStatisticsByCreator(Pageable.unpaged(), creatorId, studentName, themeName, startDate, endDate).getContent();
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
