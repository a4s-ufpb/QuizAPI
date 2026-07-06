package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.matchhistory.MatchHistoryRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.matchhistory.MatchHistoryResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.MatchHistory;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.MatchMode;
import br.ufpb.dcx.apps4society.quizapi.repository.MatchHistoryRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MatchHistoryService {
    // XP ganho: 10 por acerto + bônus de 20 se a partida foi 100% de acerto.
    private static final int XP_PER_HIT = 10;
    private static final int PERFECT_BONUS = 20;
    // Moedas (gastáveis na loja): valor menor que o XP pra manter itens cosméticos
    // como uma meta de médio prazo, não algo comprável em 1-2 partidas.
    private static final int COINS_PER_HIT = 5;
    private static final int PERFECT_COIN_BONUS = 30;

    private final MatchHistoryRepository matchHistoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final WalletService walletService;

    public MatchHistoryService(MatchHistoryRepository matchHistoryRepository, UserRepository userRepository,
                                UserService userService, WalletService walletService) {
        this.matchHistoryRepository = matchHistoryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.walletService = walletService;
    }

    public MatchHistoryResponse recordMatch(MatchHistoryRequest request, String token) {
        User user = userService.findUserByToken(token);
        MatchMode mode = MatchMode.valueOf(request.mode());
        boolean perfect = request.total() > 0 && request.score().equals(request.total());

        MatchHistory matchHistory = new MatchHistory(user, mode, request.themeName(), request.score(), request.total());
        matchHistoryRepository.save(matchHistory);

        int xpGained = request.score() * XP_PER_HIT;
        if (perfect) xpGained += PERFECT_BONUS;
        user.addXp(xpGained);
        userRepository.save(user);

        int coinsGained = request.score() * COINS_PER_HIT;
        if (perfect) coinsGained += PERFECT_COIN_BONUS;
        walletService.earn(user, coinsGained, "Partida: " + request.themeName());

        return matchHistory.entityToResponse();
    }

    public Page<MatchHistoryResponse> findMyHistory(String token, Pageable pageable) {
        User user = userService.findUserByToken(token);
        return matchHistoryRepository.findByUserOrderByPlayedAtDesc(user, pageable)
                .map(MatchHistory::entityToResponse);
    }
}
