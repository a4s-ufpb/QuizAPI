package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.ThemeFeedbackRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.feedback.ThemeFeedbackResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.ThemeFeedback;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeFeedbackRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThemeFeedbackService {
    private final ThemeFeedbackRepository repository;
    private final ThemeRepository themeRepository;
    private final UserService userService;

    public ThemeFeedbackService(ThemeFeedbackRepository repository, ThemeRepository themeRepository, UserService userService) {
        this.repository = repository;
        this.themeRepository = themeRepository;
        this.userService = userService;
    }

    public ThemeFeedbackResponse create(Long themeId, ThemeFeedbackRequest req, String token) {
        User user = userService.findUserByToken(token);
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));
        ThemeFeedback fb = new ThemeFeedback(theme, user, req.text());
        return repository.save(fb).entityToResponse();
    }

    public List<ThemeFeedbackResponse> listByTheme(Long themeId) {
        return repository.findByTheme_IdOrderByCreatedAtDesc(themeId).stream()
                .map(ThemeFeedback::entityToResponse).toList();
    }

    public long count(Long themeId) {
        return repository.countByTheme_Id(themeId);
    }
}
