package br.ufpb.dcx.apps4society.quizapi.dto.matchhistory;

import java.time.LocalDateTime;

public record MatchHistoryResponse(
        Long id,
        String mode,
        String themeName,
        int score,
        int total,
        LocalDateTime playedAt
) {
}
