package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/** Resultado de uma questão: gabarito + placar atualizado. */
public record QuestionResultView(
        Long correctAlternativeId,
        int index,
        int total,
        boolean lastQuestion,
        List<RoomStateResponse.PlayerView> scoreboard,
        List<RoomStateResponse.TeamView> teamScoreboard,
        List<PlayerAnswerView> answers,
        /** Distribuição de respostas por alternativa (gráfico estilo Kahoot). */
        List<AlternativeCountView> alternatives,
        /** Título da próxima questão (null na última) — só o líder exibe, pra decidir pular. */
        String nextQuestionTitle
) {
    /** Se cada jogador respondeu e se acertou essa questão (usado no export de estatísticas). */
    public record PlayerAnswerView(String playerId, String playerName, boolean answered, boolean correct) {}

    /** Quantos jogadores marcaram cada alternativa da questão encerrada. */
    public record AlternativeCountView(Long id, String text, int count, boolean correct) {}
}
