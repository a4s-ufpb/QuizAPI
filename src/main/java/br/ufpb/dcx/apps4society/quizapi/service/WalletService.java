package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.wallet.WalletResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.wallet.WalletTransactionResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.WalletTransaction;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.TransactionType;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.WalletTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public WalletService(WalletTransactionRepository walletTransactionRepository, UserRepository userRepository, UserService userService) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /** Credita moedas e registra a transação — chamado internamente (ex.: fim de partida), sem endpoint próprio. */
    public void earn(User user, int amount, String reason) {
        if (amount <= 0) return;
        user.addCoins(amount);
        userRepository.save(user);
        walletTransactionRepository.save(new WalletTransaction(user, TransactionType.EARN, amount, reason));
    }

    /** @return true se o débito foi aplicado (saldo suficiente). */
    public boolean spend(User user, int amount, String reason) {
        if (!user.spendCoins(amount)) return false;
        userRepository.save(user);
        walletTransactionRepository.save(new WalletTransaction(user, TransactionType.SPEND, amount, reason));
        return true;
    }

    public WalletResponse findMyWallet(String token) {
        User user = userService.findUserByToken(token);
        return new WalletResponse(user.getCoins());
    }

    public Page<WalletTransactionResponse> findMyTransactions(String token, Pageable pageable) {
        User user = userService.findUserByToken(token);
        return walletTransactionRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(WalletTransaction::entityToResponse);
    }
}
