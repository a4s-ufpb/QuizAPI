package br.ufpb.dcx.apps4society.quizapi.dto.alternative;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlternativeUpdate(
        @NotBlank(message = "Campo de resposta não pode ser vazio")
        @Size(min = 1, max = 500, message = "Número de caracteres inválido")
        String text) {
}
