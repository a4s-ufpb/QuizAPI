package br.ufpb.dcx.apps4society.quizapi.dto.question;

import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeResponse;

import java.util.List;

/**
 * Questão para jogar o quiz single-player. Imagens já vêm como URLs do MinIO
 * (leves, sem precisar de fetch sob demanda como quando eram base64).
 */
public record QuestionQuizResponse(
        Long id,
        String title,
        String imageUrl,
        String imageOneUrl,
        String imageTwoUrl,
        String imagesOrder,
        List<AlternativeResponse> alternatives
) {
}
