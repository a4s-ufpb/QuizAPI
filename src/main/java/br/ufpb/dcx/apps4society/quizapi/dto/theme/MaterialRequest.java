package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.MaterialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Material de apoio de um tema (vídeo, arquivo ou site). */
public record MaterialRequest(
        @NotBlank(message = "O nome do material não pode ser vazio")
        String name,
        @NotBlank(message = "O link do material não pode ser vazio")
        String link,
        @NotNull(message = "O tipo do material é obrigatório")
        MaterialType type
) {
}
