package br.ufpb.dcx.apps4society.quizapi.dto.score;

import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;

public record GlobalRankingResponse(
        UserResponse user,
        double totalScore
) {
}
