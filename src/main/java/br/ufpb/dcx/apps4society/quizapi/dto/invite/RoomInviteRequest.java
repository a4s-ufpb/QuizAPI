package br.ufpb.dcx.apps4society.quizapi.dto.invite;

import jakarta.validation.constraints.NotBlank;

public record RoomInviteRequest(
        @NotBlank String roomCode
) {
}
