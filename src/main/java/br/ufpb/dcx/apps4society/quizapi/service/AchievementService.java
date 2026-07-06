package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.achievement.AchievementResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.FriendshipStatus;
import br.ufpb.dcx.apps4society.quizapi.repository.FriendshipRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.MatchHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Conquistas calculadas dinamicamente a partir do histórico de partidas,
// curtidas, nível e amizades (sem tabela própria de "desbloqueados" —
// simplicidade em troca de não registrar a data exata do desbloqueio).
@Service
public class AchievementService {
    private final MatchHistoryRepository matchHistoryRepository;
    private final FriendshipRepository friendshipRepository;

    public AchievementService(MatchHistoryRepository matchHistoryRepository,
                              FriendshipRepository friendshipRepository) {
        this.matchHistoryRepository = matchHistoryRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public List<AchievementResponse> findAchievements(User user) {
        long totalMatches = matchHistoryRepository.countByUser(user);
        long perfectMatches = matchHistoryRepository.countPerfectMatches(user);
        int likes = user.getLikes();
        int level = user.getLevel();
        long friends = friendshipRepository
                .findAllByUserAndStatus(user, FriendshipStatus.ACCEPTED).size();

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
                        "Alcance o nível 5", level >= 5
                ),
                new AchievementResponse(
                        "LEVEL_10", "Lenda", "Alcance o nível 10", level >= 10
                ),
                new AchievementResponse(
                        "LEVEL_25", "Veterano Lendário",
                        "Alcance o nível 25", level >= 25
                ),
                new AchievementResponse(
                        "FIRST_LIKE", "Simpático",
                        "Receba sua primeira curtida", likes >= 1
                ),
                new AchievementResponse(
                        "TEN_LIKES", "Popular",
                        "Receba 10 curtidas", likes >= 10
                ),
                new AchievementResponse(
                        "FIFTY_LIKES", "Ídolo",
                        "Receba 50 curtidas", likes >= 50
                ),
                new AchievementResponse(
                        "FIRST_FRIEND", "Companheirismo",
                        "Adicione seu primeiro amigo", friends >= 1
                ),
                new AchievementResponse(
                        "FIVE_FRIENDS", "Sociável",
                        "Tenha 5 amigos", friends >= 5
                ),
                new AchievementResponse(
                        "TEN_FRIENDS", "Rede de Amigos",
                        "Tenha 10 amigos", friends >= 10
                )
        );
    }
}
