package br.ufpb.dcx.apps4society.quizapi.dto.wallet;

import java.time.LocalDateTime;

public record WalletTransactionResponse(
        Long id,
        String type,
        int amount,
        String reason,
        LocalDateTime createdAt
) {
}
