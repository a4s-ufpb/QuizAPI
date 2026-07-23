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
        String pendingPowerUp,
        /**
         * Prévia da próxima questão (só quando status == BETWEEN e há próxima).
         * Serve ao líder para pré-visualizar/decidir pular; os jogadores não a
         * exibem — a questão só aparece quando o líder confirma "Próxima".
         */
        NextQuestionPreview nextQuestion,
        /** Quantas questões o líder já pulou desde o último resultado (aviso aos jogadores). */
        int skippedCount
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
            String userUuid,
            boolean eliminated,
            String title,
            String frame,
            String banner,
            String font,
            String nameStyle,
            String nameEffect,
            int correctCount
    ) {}

    public record TeamView(String id, String name, int score, String avatar, String captainId) {}

    /** Prévia completa da próxima questão (só o líder renderiza). */
    public record NextQuestionPreview(
            Long id,
            String title,
            String imageUrl,
            String imageOneUrl,
            String imageTwoUrl,
            String imagesOrder,
            List<AlternativePreview> alternatives
    ) {
        public record AlternativePreview(Long id, String text) {}
    }
}
