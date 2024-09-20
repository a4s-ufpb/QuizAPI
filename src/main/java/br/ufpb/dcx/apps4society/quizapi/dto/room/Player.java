package br.ufpb.dcx.apps4society.quizapi.dto.room;

import br.ufpb.dcx.apps4society.quizapi.entity.User;

import java.util.UUID;

public record Player(
        UUID playerId,
        String name
) {
}
