package br.ufpb.dcx.apps4society.quizapi.dto.statistic;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatisticRequest(
        @NotBlank(message = "Campo studentName não pode ser vazio")
        @Size(min = 3, max = 30, message = "Número de caracteres inválido")
        String studentName,
        @NotBlank(message = "Campo themeName não pode ser vazio")
        @Size(min = 3, max = 20, message = "Número de caracteres inválido")
        String themeName,
        @NotNull(message = "Esse campo não pode estar vazio")
        Double percentagemOfHits,
        GameMode gameMode
) {
    // Compat: chamada antiga sem gameMode assume SINGLE_PLAYER.
    public StatisticRequest(String studentName, String themeName, Double percentagemOfHits) {
        this(studentName, themeName, percentagemOfHits, GameMode.SINGLE_PLAYER);
    }
}
