package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/**
 * Questão enviada aos jogadores durante a partida — SEM o gabarito. As
 * imagens já são URLs do MinIO (leves), então vão direto no broadcast STOMP,
 * sem precisar de fetch sob demanda.
 */
public record QuestionView(
        Long id,
        String title,
        String imageUrl,
        String imageOneUrl,
        String imageTwoUrl,
        String imagesOrder,
        int index,
        int total,
        int timeSeconds,
        long startAt,
        List<AlternativeView> alternatives,
        String activePower
) {
    public record AlternativeView(Long id, String text) {}
}
