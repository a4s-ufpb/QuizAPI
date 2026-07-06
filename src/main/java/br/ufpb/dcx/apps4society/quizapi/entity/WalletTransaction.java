package br.ufpb.dcx.apps4society.quizapi.entity;

import br.ufpb.dcx.apps4society.quizapi.dto.wallet.WalletTransactionResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.TransactionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "tb_wallet_transaction")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    private int amount;
    private String reason;
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletTransaction() {}

    public WalletTransaction(User user, TransactionType type, int amount, String reason) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
    }

    public WalletTransactionResponse entityToResponse() {
        return new WalletTransactionResponse(id, type.name(), amount, reason, createdAt);
    }

    public User getUser() {
        return user;
    }
}
