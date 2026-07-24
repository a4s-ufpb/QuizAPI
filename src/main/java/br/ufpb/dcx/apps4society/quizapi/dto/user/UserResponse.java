package br.ufpb.dcx.apps4society.quizapi.dto.user;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID uuid,
        String name,
        String email,
        Role role,
        int likes,
        int xp,
        int level,
        int coins,
        String equippedTitle,
        String equippedFrame,
        String equippedBanner,
        String equippedFont,
        String equippedNameStyle,
        String equippedNameEffect
) {
}
