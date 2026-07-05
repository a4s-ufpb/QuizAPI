package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/** Estado da sala visível para todos (não inclui gabarito da questão atual). */
public record RoomStateResponse(
        String code,
        String hostId,
        Long themeId,
        String themeName,
        String themeImageUrl,
        GameConfig config,
        String status,
        List<PlayerView> players,
        List<TeamView> teams,
        int currentQuestionIndex,
        int totalQuestions,
        String pendingPowerUp
) {
    public record PlayerView(
            String id,
            String name,
            boolean host,
            boolean ready,
            String teamId,
            int score,
            String avatar,
            boolean captain,
            String userUuid
    ) {}

    public record TeamView(String id, String name, int score, String avatar, String captainId) {}
}
