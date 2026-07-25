package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.StatisticFeedbackRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.feedback.StatisticFeedbackResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticFeedback;
import br.ufpb.dcx.apps4society.quizapi.entity.StatisticPerConclusion;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticFeedbackRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticFeedbackService {
    private final StatisticFeedbackRepository repository;
    private final StatisticService statisticService;
    private final UserService userService;

    public StatisticFeedbackService(StatisticFeedbackRepository repository,
                                    StatisticService statisticService,
                                    UserService userService) {
        this.repository = repository;
        this.statisticService = statisticService;
        this.userService = userService;
    }

    public StatisticFeedbackResponse create(Long statisticId, StatisticFeedbackRequest req, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);
        StatisticPerConclusion st = statisticService.getById(statisticId);
        if (st == null) throw new RuntimeException("Estatística não encontrada");
        if (!st.getCreatorId().equals(user.getUuid())) {
            throw new UserNotHavePermissionException("Só o criador do tema pode enviar feedback dessa conclusão");
        }
        return repository.save(new StatisticFeedback(st, user, req.text())).entityToResponse();
    }

    public List<StatisticFeedbackResponse> listByStatistic(Long statisticId) {
        return repository.findByStatistic_IdOrderByCreatedAtDesc(statisticId).stream()
                .map(StatisticFeedback::entityToResponse).toList();
    }

    public List<StatisticFeedbackResponse> listMine(String token) {
        User user = userService.findUserByToken(token);
        return repository.findByStudentUuid(user.getUuid()).stream()
                .map(StatisticFeedback::entityToResponse).toList();
    }

    public List<StatisticFeedbackResponse> listByCreator(String token) {
        User user = userService.findUserByToken(token);
        return repository.findByCreator_UuidOrderByCreatedAtDesc(user.getUuid()).stream()
                .map(StatisticFeedback::entityToResponse).toList();
    }
}
