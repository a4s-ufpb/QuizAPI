package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/** Resultado de uma questão: gabarito + placar atualizado. */
public record QuestionResultView(
        Long correctAlternativeId,
        int index,
        int total,
        boolean lastQuestion,
        List<RoomStateResponse.PlayerView> scoreboard,
        List<RoomStateResponse.TeamView> teamScoreboard
) {}
