package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.achievement.AchievementResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.MatchHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Conquistas calculadas dinamicamente a partir do histórico de partidas
// (sem tabela própria de "desbloqueados" — simplicidade em troca de não
// registrar a data exata do desbloqueio).
@Service
public class AchievementService {
    private final MatchHistoryRepository matchHistoryRepository;

    public AchievementService(MatchHistoryRepository matchHistoryRepository) {
        this.matchHistoryRepository = matchHistoryRepository;
    }

    public List<AchievementResponse> findAchievements(User user) {
        long totalMatches = matchHistoryRepository.countByUser(user);
        long perfectMatches = matchHistoryRepository.countPerfectMatches(user);

        return List.of(
                new AchievementResponse(
                        "FIRST_MATCH", "Primeira Partida",
                        "Jogue sua primeira partida", totalMatches >= 1
                ),
                new AchievementResponse(
                        "TEN_MATCHES", "Veterano",
                        "Jogue 10 partidas", totalMatches >= 10
                ),
                new AchievementResponse(
                        "FIFTY_MATCHES", "Dedicado",
                        "Jogue 50 partidas", totalMatches >= 50
                ),
                new AchievementResponse(
                        "PERFECT_SCORE", "Perfeição",
                        "Acerte 100% das questões em uma partida", perfectMatches >= 1
                ),
                new AchievementResponse(
                        "FIVE_PERFECT", "Mestre do Quiz",
                        "Acerte 100% das questões em 5 partidas", perfectMatches >= 5
                ),
                new AchievementResponse(
                        "LEVEL_5", "Ascendente",
                        "Alcance o nível 5", user.getLevel() >= 5
                ),
                new AchievementResponse(
                        "LEVEL_10", "Lenda", "Alcance o nível 10", user.getLevel() >= 10
                )
        );
    }
}
