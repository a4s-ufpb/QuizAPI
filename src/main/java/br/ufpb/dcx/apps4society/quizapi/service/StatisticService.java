package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StatisticService {
    private final StatisticRepository statisticRepository;
    private final UserRepository userRepository;
    private final ThemeRepository themeRepository;

    public StatisticService(StatisticRepository statisticRepository, UserRepository userRepository, ThemeRepository themeRepository) {
        this.statisticRepository = statisticRepository;
        this.userRepository = userRepository;
        this.themeRepository = themeRepository;
    }

    public StatisticResponse insertStatistic(StatisticRequest statisticRequest) throws UserNotHavePermissionException {
        User creator = userRepository.findById(statisticRequest.creatorId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        Theme theme = themeRepository.findByNameIgnoreCase(statisticRequest.themeName());

        if (creator.userNotHavePermission(theme.getCreator())) {
            throw new UserNotHavePermissionException("O usuário informado não é o criador do tema");
        }

        StatisticPerConclusion statistic = new StatisticPerConclusion(statisticRequest);

        statisticRepository.save(statistic);

        return statistic.entityToResponse();
    }

    public Page<StatisticResponse> findAllStatisticsByCreator(Pageable pageable, UUID creatorId, String studentName, String themeName) {
        Page<StatisticPerConclusion> statistic;

        if (!studentName.isBlank() && themeName.isBlank()) {
            statistic = statisticRepository.findByCreatorIdAndStudentName(pageable, creatorId, studentName);
        } else if (studentName.isBlank() && !themeName.isBlank()) {
            statistic = statisticRepository.findByCreatorIdAndThemeName(pageable, creatorId, themeName);
        } else if (!studentName.isBlank() && !themeName.isBlank()) {
            statistic = statisticRepository.findByCreatorIdAndStudentNameAndThemeName(pageable, creatorId, studentName, themeName);
        } else {
            statistic = statisticRepository.findByCreatorId(pageable, creatorId);
        }

        return statistic.map(StatisticPerConclusion::entityToResponse);
    }

}
