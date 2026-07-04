package br.ufpb.dcx.apps4society.quizapi.dto.response;

/** Resumo das respostas do próprio usuário logado (como jogador), com filtros de tema/data. */
public record MySummaryDTO(
        long totalQuizzesFinished,
        long totalCorrectAnswers,
        long totalWrongAnswers
) {
}
