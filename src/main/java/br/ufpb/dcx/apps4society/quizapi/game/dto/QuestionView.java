package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/**
 * Questão enviada aos jogadores durante a partida — SEM o gabarito e SEM os
 * base64 de imagem (grandes demais pra ir em todo broadcast STOMP). Quando
 * {@code imagesOrder} indica upload (IMAGE_1/IMAGE_2), o cliente busca as
 * imagens via GET /question/{id}/images.
 */
public record QuestionView(
        Long id,
        String title,
        String imageUrl,
        String imagesOrder,
        int index,
        int total,
        int timeSeconds,
        long startAt,
        List<AlternativeView> alternatives
) {
    public record AlternativeView(Long id, String text) {}
}
