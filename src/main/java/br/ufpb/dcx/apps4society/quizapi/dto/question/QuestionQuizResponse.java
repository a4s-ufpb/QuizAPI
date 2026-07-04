package br.ufpb.dcx.apps4society.quizapi.dto.question;

import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeResponse;

import java.util.List;

/**
 * Questão para jogar o quiz single-player, SEM os base64 de imagem — o
 * cliente busca a imagem da questão atual sob demanda via
 * GET /question/{id}/images, em vez de baixar todas as 10 de uma vez.
 */
public record QuestionQuizResponse(
        Long id,
        String title,
        String imageUrl,
        String imagesOrder,
        List<AlternativeResponse> alternatives
) {
}
