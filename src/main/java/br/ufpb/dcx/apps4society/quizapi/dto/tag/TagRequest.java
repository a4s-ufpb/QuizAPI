package br.ufpb.dcx.apps4society.quizapi.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
        @NotBlank(message = "Nome da tag não pode ser vazio")
        @Size(min = 1, max = 60, message = "Número de caracteres inválido")
        String name
) {}
