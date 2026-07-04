package br.ufpb.dcx.apps4society.quizapi.dto.user;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdate(
        @NotNull Role role
) {
}
