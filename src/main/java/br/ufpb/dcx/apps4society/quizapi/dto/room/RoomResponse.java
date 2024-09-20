package br.ufpb.dcx.apps4society.quizapi.dto.room;

import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;

import java.util.List;
import java.util.UUID;

public record RoomResponse(
         UUID roomId,
         UserResponse creator,
         Long selectedQuizId,
         Boolean started,
         List<Player> users
) {
}
