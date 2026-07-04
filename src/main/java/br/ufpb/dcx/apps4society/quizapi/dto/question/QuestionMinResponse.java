package br.ufpb.dcx.apps4society.quizapi.dto.question;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;

public record QuestionMinResponse(
        Long id,
        String title,
        String imageUrl,
        String imageBase64One,
        String imageBase64Two,
        String imagesOrder,
        ThemeResponse theme
) {
}
