package br.ufpb.dcx.apps4society.quizapi.dto.response;

import jakarta.validation.constraints.NotNull;

public record ResponseItemRequest(
        @NotNull Long questionId,
        @NotNull Long alternativeId
) {
}
