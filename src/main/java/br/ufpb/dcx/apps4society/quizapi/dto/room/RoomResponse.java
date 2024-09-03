package br.ufpb.dcx.apps4society.quizapi.dto.room;

import java.util.UUID;

public record RoomResponse(
        UUID roomId,
        UUID creatorId,
        Long selectedQuizId,
        Boolean started
) {
}
