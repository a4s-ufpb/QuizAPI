package br.ufpb.dcx.apps4society.quizapi.dto.room.ws;

import java.util.UUID;

public record StartQuizRequest(
        UUID roomId
) {
}
