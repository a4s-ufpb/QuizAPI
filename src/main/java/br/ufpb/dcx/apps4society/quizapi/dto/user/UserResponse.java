package br.ufpb.dcx.apps4society.quizapi.dto.user;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID uuid,
        String name,
        String email,
        Role role,
        int likes
) {
}
