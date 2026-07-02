package br.ufpb.dcx.apps4society.quizapi.dto.alternative;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlternativeRequest(
        @NotBlank(message = "Campo de resposta não pode ser vazio")
        @Size(min = 1, max = 500, message = "Número de caracteres inválido")
        String text,
        @NotNull(message = "Esse campo não pode ser vazio")
        Boolean correct) {
}
