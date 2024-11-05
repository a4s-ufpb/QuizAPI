package br.ufpb.dcx.apps4society.quizapi.dto.room.ws;

import java.util.UUID;

public record RoomAndPlayerId(
        UUID roomId,
        UUID playerId
) {
}
