package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/** Estado completo do torneio (bracket + jogadores), consultado via polling pelo frontend. */
public record TournamentStateResponse(
        String code,
        String hostId,
        String name,
        Long themeId,
        String themeName,
        String status,
        List<PlayerView> players,
        List<List<MatchView>> rounds,
        String championId,
        /** Capacidade máxima configurada (4/8/16). */
        int maxPlayers,
        /** Nº de jogadores fixado ao travar as chaves (0 enquanto no lobby). */
        int bracketSize,
        /** Quiz de cada rodada do chaveamento (só após travar as chaves). */
        List<RoundThemeView> roundThemes
) {
    public record PlayerView(String id, String name, boolean host, boolean eliminated,
                             String title, String frame, String banner,
                             String font, String nameStyle, String nameEffect) {}

    /** Tema escolhido para uma rodada do chaveamento (themeId/name null = ainda não escolhido). */
    public record RoundThemeView(int roundIndex, String label, Long themeId, String themeName) {}

    public record MatchView(
            String id,
            String player1Id,
            String player2Id,
            String winnerId,
            String roomCode,
            String status
    ) {}
}
