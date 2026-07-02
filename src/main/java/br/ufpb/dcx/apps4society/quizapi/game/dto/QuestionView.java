package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/** Questão enviada aos jogadores durante a partida — SEM o gabarito. */
public record QuestionView(
        Long id,
        String title,
        String imageUrl,
        int index,
        int total,
        int timeSeconds,
        long startAt,
        List<AlternativeView> alternatives
) {
    public record AlternativeView(Long id, String text) {}
}
