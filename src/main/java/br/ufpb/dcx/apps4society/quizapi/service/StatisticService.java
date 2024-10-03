package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StudentName;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.ThemeName;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StatisticService {
    private final StatisticRepository statisticRepository;
    private final ThemeRepository themeRepository;

    public StatisticService(StatisticRepository statisticRepository, ThemeRepository themeRepository) {
        this.statisticRepository = statisticRepository;
        this.themeRepository = themeRepository;
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
        Page<StatisticPerConclusion> statistic;

        boolean hasStudentName = !studentName.isBlank();
        boolean hasThemeName = !themeName.isBlank();
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasStudentName && !hasThemeName && hasDateRange) {
            // Buscar por creatorId, studentName e intervalo de datas
            statistic = statisticRepository.findByCreatorIdAndStudentNameAndDateRange(pageable, creatorId, studentName, startDate, endDate);
        } else if (!hasStudentName && hasThemeName && hasDateRange) {
            // Buscar por creatorId, themeName e intervalo de datas
            statistic = statisticRepository.findByCreatorIdAndThemeNameAndDateRange(pageable, creatorId, themeName, startDate, endDate);
        } else if (hasStudentName && hasThemeName && hasDateRange) {
            // Buscar por creatorId, themeName, studentName e intervalo de datas
            statistic = statisticRepository.findByCreatorIdAndThemeNameAndStudentNameAndDateRange(pageable, creatorId, themeName, studentName, startDate, endDate);
        } else if (hasStudentName && !hasThemeName) {
            // Buscar por creatorId e studentName (sem data)
            statistic = statisticRepository.findByCreatorIdAndStudentName(pageable, creatorId, studentName);
        } else if (!hasStudentName && hasThemeName) {
            // Buscar por creatorId e themeName (sem data)
            statistic = statisticRepository.findByCreatorIdAndThemeName(pageable, creatorId, themeName);
        } else if (hasDateRange) {
            // Buscar por creatorId e intervalo de datas
            statistic = statisticRepository.findByCreatorIdAndDateRange(pageable, creatorId, startDate, endDate);
        } else if (hasStudentName && hasThemeName && !hasDateRange) {
            // Buscar por creatorId, themeName, studentName
            statistic = statisticRepository.findByCreatorIdAndThemeNameAndStudentName(pageable, creatorId, themeName, studentName);
        } else {
            // Buscar apenas por creatorId
            statistic = statisticRepository.findByCreatorId(pageable, creatorId);
        }

        return statistic.map(StatisticPerConclusion::entityToResponse);
    }

    public List<StudentName> findDistinctStudentNameByCreatorId(UUID creatorId) {
        Set<StudentName> seenThemesAndStudents = new HashSet<>();

        return statisticRepository
                .findByCreatorId(creatorId)
                .stream()
                .map(sts -> new StudentName(sts.getStudentName()))
                .filter(seenThemesAndStudents::add)
                .toList();
    }

    public List<ThemeName> findDistinctThemeNameByCreatorId(UUID creatorId) {
        Set<ThemeName> seenThemesAndStudents = new HashSet<>();

        return statisticRepository
                .findByCreatorId(creatorId)
                .stream()
                .map(sts -> new ThemeName(sts.getThemeName()))
                .filter(seenThemesAndStudents::add)
                .toList();
    }


}
