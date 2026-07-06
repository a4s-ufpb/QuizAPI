package br.ufpb.dcx.apps4society.quizapi.dto.user;

import br.ufpb.dcx.apps4society.quizapi.dto.achievement.AchievementResponse;

import java.util.List;
import java.util.UUID;

/**
 * Perfil público de um jogador — visível por qualquer visitante, sem exigir
 * login. Deliberadamente não expõe email nem moedas (só o que é "de vitrine").
 */
public record PublicProfileResponse(
        UUID uuid,
        String name,
        int likes,
        int xp,
        int level,
        String equippedTitle,
        String equippedFrame,
        String equippedBanner,
        List<AchievementResponse> achievements
) {
}
