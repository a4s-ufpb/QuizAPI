package br.ufpb.dcx.apps4society.quizapi.dto.room;

import java.util.UUID;

public record RoomRequest(
        UUID creatorId
) {
}
