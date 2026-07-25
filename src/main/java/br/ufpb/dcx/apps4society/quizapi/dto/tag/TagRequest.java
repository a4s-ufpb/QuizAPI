package br.ufpb.dcx.apps4society.quizapi.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank(message = "Nome da tag não pode ser vazio")
        @Size(min = 3, max = 60, message = "A tag deve ter no mínimo 3 caracteres")
        String name
) {}
