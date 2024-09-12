package br.ufpb.dcx.apps4society.quizapi.dto.statistic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StatisticRequest(
        @NotNull(message = "Esse campo não pode estar vazio")
        UUID creatorId,
        @NotBlank(message = "Campo studentName não pode ser vazio")
        @Size(min = 3, max = 30, message = "Número de caracteres inválido")
        String studentName,
        @NotBlank(message = "Campo themeName não pode ser vazio")
        @Size(min = 3, max = 20, message = "Número de caracteres inválido")
        String themeName,
        @NotNull(message = "Esse campo não pode estar vazio")
        Double percentagemOfHits
) {
}
