package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import java.util.List;

public record ThemeResponse(
        Long id,
        String name,
        String imageUrl,
        /** Conteúdos abordados pelo tema (texto livre). */
        String description,
        List<MaterialResponse> materials) {

    // Compat: usos que só precisam de id/nome/imagem (sem conteúdos/materiais).
    public ThemeResponse(Long id, String name, String imageUrl) {
        this(id, name, imageUrl, null, List.of());
    }
}
