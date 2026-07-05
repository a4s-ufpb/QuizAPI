package br.ufpb.dcx.apps4society.quizapi.dto.friendship;

import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;

public record FriendshipResponse(
        Long id,
        UserResponse requester,
        UserResponse addressee,
        String status
) {
}
