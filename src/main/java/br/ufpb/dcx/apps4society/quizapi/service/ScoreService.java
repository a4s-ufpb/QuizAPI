package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.score.GlobalRankingResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Score;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.GlobalRankingProjection;
import br.ufpb.dcx.apps4society.quizapi.repository.ScoreRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ScoreNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScoreService {
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final ThemeRepository themeRepository;

    public ScoreService(ScoreRepository scoreRepository, UserRepository userRepository, ThemeRepository themeRepository) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.themeRepository = themeRepository;
    }

    public ScoreResponse insertScore(ScoreRequest scoreRequest, UUID userId, Long themeId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new ThemeNotFoundException("Tema não encontrado"));

        Score score = new Score(scoreRequest.numberOfHits(), scoreRequest.totalTime(), user, theme);
        scoreRepository.save(score);

        return score.entityToResponse();
    }

    public List<ScoreResponse> findRankingByTheme(Long themeId){
        List<Score> ranking = scoreRepository.findByTheme(themeId);

        if (ranking.isEmpty()){
            throw new ScoreNotFoundException("Nenhuma pontuação cadastrada nesse quiz");
        }

        return ranking.stream().map(Score::entityToResponse).toList();
    }

    public Page<GlobalRankingResponse> findGlobalRanking(String period, Pageable pageable) {
        Page<GlobalRankingProjection> ranking = "WEEK".equalsIgnoreCase(period)
                ? scoreRepository.findGlobalRankingSince(LocalDateTime.now().minusWeeks(1), pageable)
                : scoreRepository.findGlobalRankingAll(pageable);

        return ranking.map(row -> new GlobalRankingResponse(row.getUser().entityToResponse(), row.getTotal()));
    }
}
