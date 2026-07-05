package br.ufpb.dcx.apps4society.quizapi.dto.achievement;

public record AchievementResponse(
        String code,
        String name,
        String description,
        boolean unlocked
) {
}
