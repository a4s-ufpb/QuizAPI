package br.ufpb.dcx.apps4society.quizapi.dto.matchhistory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MatchHistoryRequest(
        @NotNull String mode,
        @NotBlank String themeName,
        @NotNull Integer score,
        @NotNull Integer total
) {
}
