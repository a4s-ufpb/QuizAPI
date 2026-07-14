package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.MaterialType;

public record MaterialResponse(
        Long id,
        String name,
        String link,
        MaterialType type
) {
}
