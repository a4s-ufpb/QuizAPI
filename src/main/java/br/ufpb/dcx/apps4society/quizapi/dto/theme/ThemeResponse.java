package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagResponse;

import java.util.List;

public record ThemeResponse(
        Long id,
        String name,
        String imageUrl,
        /** Conteúdos abordados pelo tema (texto livre). */
        String description,
        List<MaterialResponse> materials,
        List<TagResponse> tags,
        Long likeCount,
        Boolean likedByMe) {

    // Compat: usos que só precisam de id/nome/imagem.
    public ThemeResponse(Long id, String name, String imageUrl) {
        this(id, name, imageUrl, null, List.of(), List.of(), 0L, false);
    }

    public ThemeResponse(Long id, String name, String imageUrl, String description, List<MaterialResponse> materials) {
        this(id, name, imageUrl, description, materials, List.of(), 0L, false);
    }
}
